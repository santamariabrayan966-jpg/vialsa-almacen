import sys,json
from pathlib import Path
OUT=Path(__file__).resolve().parent
SRC=OUT.parent/'articulated-v1'
sys.path.insert(0,str(OUT.parents[2]/'verification'/'dental-python-deps'))
import numpy as np
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import dijkstra
from scipy.spatial import cKDTree
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

PIXELS={
 'lower':[(235,267),(247,387),(264,492),(296,588),(340,680),(389,748),(445,806),(501,840),(560,847),(618,833),(673,805),(728,768),(783,703),(843,631),(884,540),(922,443),(979,345)],
 'upper':[(224,318),(272,434),(324,551),(373,652),(410,742),(481,804),(548,840),(627,843),(704,828),(772,790),(836,747),(875,681),(910,585),(938,478),(954,355),(970,230)]
}

FDI={
 'upper':[28,27,26,25,24,23,22,21,11,12,13,14,15,16,17,18],
 'lower':[48,47,46,45,44,43,42,41,'EXTRA_L_01',31,32,33,34,35,36,37,38]
}

def segment(arch):
    d=np.load(SRC/f'{arch}.npz');v,f,n=d['vertices'],d['faces'],d['normals']
    edges=np.unique(np.sort(np.concatenate([f[:,[0,1]],f[:,[1,2]],f[:,[2,0]]]),axis=1),axis=0)
    a,b=edges.T;length=np.linalg.norm(v[a]-v[b],axis=1)
    adjacency=coo_matrix((np.ones(2*len(a)),(np.r_[a,b],np.r_[b,a])),shape=(len(v),len(v))).tocsr()
    degree=np.maximum(np.asarray(adjacency.sum(axis=1)).ravel(),1)
    lap=(adjacency@v)/degree[:,None]-v
    mean_length=np.bincount(np.r_[a,b],weights=np.r_[length,length],minlength=len(v))/degree
    curv=np.sum(lap*n,axis=1)/np.maximum(mean_length**2,1e-8)
    # Concave cervical grooves slow diffusion; narrow fissures inside each
    # crown are crossed by multiple seeds on its visible occlusal surface.
    barrier=np.minimum(np.maximum(curv,0),3.0)
    weights=length*(1+18*((barrier[a]+barrier[b])*.5)**2)
    graph=coo_matrix((np.r_[weights,weights],(np.r_[a,b],np.r_[b,a])),shape=adjacency.shape).tocsr()
    centers=np.array(PIXELS[arch],dtype=float)
    centers[:,0]=(centers[:,0]-600)*76/1200*(-1 if arch=='upper' else 1)
    centers[:,1]=(550-centers[:,1])*76/1200
    facing=-1 if arch=='upper' else 1
    dxy=np.linalg.norm(v[:,None,:2]-centers[None,:,:],axis=2)
    occlusal=(n[:,2]*facing>.15)&(v[:,2]*facing>-5)
    markers=[];origins=[]
    for i,c in enumerate(centers):
        index=np.flatnonzero(occlusal & (dxy[:,i]<1.7))
        if not len(index):index=np.argsort(dxy[:,i])[:20]
        # Don't seed the dark fissures themselves.
        good=index[curv[index]<.4]
        if len(good):index=good
        markers.append(index)
        origins.append(v[index].mean(axis=0))
    near=np.argmin(dxy,axis=1)
    depth=v[:,2]*(-facing)
    elliptical=[]
    for i,c in enumerate(centers):
        before=centers[max(0,i-1)];after=centers[min(len(centers)-1,i+1)]
        tangent=(after-before)/np.linalg.norm(after-before)
        radial=np.array([-tangent[1],tangent[0]])
        spacings=[np.linalg.norm(c-other)for other in [before,after]if np.linalg.norm(c-other)>.01]
        halfwidth=np.mean(spacings)*.57
        code=FDI[arch][i];number=int(code)%10 if isinstance(code,int) else 1
        crosswidth=5.3 if number>=6 else 4.0 if number>=4 else 3.7 if number==3 else 3.2
        delta=v[:,:2]-c
        elliptical.append(np.sqrt((delta@tangent/halfwidth)**2+(delta@radial/crosswidth)**2))
    ellipse=np.stack(elliptical,axis=1)
    # Base and exposed gum surfaces provide independent gum labels.
    gum=(depth>(8 if arch=='upper' else 7)) | ((ellipse.min(axis=1)>1.28)&(depth>1.5))
    gumidx=np.flatnonzero(gum)
    gumdist=dijkstra(graph,directed=False,indices=gumidx,min_only=True)
    distances=[gumdist*.82]
    for idx in markers:distances.append(dijkstra(graph,directed=False,indices=idx,min_only=True))
    distances=np.stack(distances)
    labels=np.argmin(distances,axis=0).astype(np.int16)-1
    # Keep tiny disconnected source fragments with the gum.
    labels[~np.isfinite(distances.min(axis=0))]=-1
    # Smooth only the class boundary; keep fixed seeds on the crown and gum.
    fixed=np.zeros(len(v),dtype=bool);fixed[gumidx]=True
    for idx in markers:fixed[idx]=True
    for _ in range(3):
        votes=np.stack([np.asarray(adjacency@(labels==k).astype(float)).ravel()for k in range(-1,len(centers))])
        majority=votes.argmax(axis=0)-1
        change=(votes.max(axis=0)>degree*.60)&~fixed
        labels[change]=majority[change]
    np.savez_compressed(OUT/f'{arch}_segmented.npz',vertices=v,faces=f,labels=labels,normals=n)
    metadata={'arch':arch,'ids':FDI[arch],'seed_centers':centers.tolist(),'origins':np.array(origins).tolist(),
              'counts':{str(code):int((labels==i).sum())for i,code in enumerate(FDI[arch])}}
    (OUT/f'{arch}_segmentation.json').write_text(json.dumps(metadata,indent=2))
    fig,ax=plt.subplots(figsize=(11,10))
    order=np.argsort(v[:,2]*facing)
    ax.scatter(v[order,0],v[order,1],c=labels[order],s=1,cmap='tab20',vmin=-1,vmax=18)
    for c,code in zip(centers,FDI[arch]):ax.text(*c,str(code),fontsize=9,ha='center',color='black',bbox={'facecolor':'white','alpha':.8,'pad':1})
    ax.set_aspect('equal');ax.set_title(arch+' - tooth labels / grey = gingiva')
    fig.savefig(OUT/f'{arch}_segmentation.png',dpi=140);plt.close(fig)
    print(arch,metadata['counts'],flush=True)

def plot_seeds():
    for arch,points in PIXELS.items():
        image=plt.imread(SRC/f'source_{arch}.png')
        fig,ax=plt.subplots(figsize=(12,11));ax.imshow(image)
        for i,(x,y) in enumerate(points):
            ax.plot(x,y,'o',color='red',ms=5);ax.text(x+5,y,str(i+1),color='yellow',fontsize=15,weight='bold')
        ax.set_xticks(np.arange(0,1201,100));ax.set_yticks(np.arange(0,1101,100));ax.grid(alpha=.5)
        fig.savefig(OUT/f'{arch}_seed_review.png',dpi=120);plt.close(fig)

if __name__=='__main__':
    if '--seeds' in sys.argv:plot_seeds()
    else:
        for arch in ['upper','lower']:segment(arch)
