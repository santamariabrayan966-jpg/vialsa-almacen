"""Local mesh diagnostics and assisted tooth segmentation; original source is read-only."""
import sys
from pathlib import Path
OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(OUT.parents[2]/'verification'/'dental-python-deps'))
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import dijkstra
from scipy.spatial import cKDTree


def load_arch(name):
    data=np.load(OUT/(name+'.npz'))
    v,f,n=data['vertices'],data['faces'],data['normals']
    edges=np.unique(np.sort(np.concatenate([f[:,[0,1]],f[:,[1,2]],f[:,[2,0]]]),axis=1),axis=0)
    a,b=edges.T
    length=np.linalg.norm(v[a]-v[b],axis=1)
    adjacency=coo_matrix((np.ones(2*len(a)),(np.r_[a,b],np.r_[b,a])),shape=(len(v),len(v))).tocsr()
    degree=np.maximum(np.asarray(adjacency.sum(axis=1)).ravel(),1)
    lap=(adjacency@v)/degree[:,None]-v
    curv=np.sum(lap*n,axis=1)/(np.asarray(coo_matrix((np.r_[length,length],(np.r_[a,b],np.r_[b,a])),shape=adjacency.shape).tocsr().sum(axis=1)).ravel()/degree)**2
    curv=np.nan_to_num(curv)
    return v,f,n,edges,length,adjacency,curv


def diagnostics(name):
    v,f,n,edges,length,adj,curv=load_arch(name)
    # Crown-facing surface only, determined from the normalized model.
    facing=-1 if name=='upper' else 1
    visible=n[:,2]*facing>0.15
    fig,axs=plt.subplots(1,2,figsize=(16,8),layout='constrained')
    for ax,c,title in [(axs[0],v[:,2],'Height (mm)'),(axs[1],curv,'Signed concavity')]:
        order=np.argsort(v[:,2]*facing)
        order=order[visible[order]]
        s=ax.scatter(v[order,0],v[order,1],c=c[order],s=1,cmap='viridis' if ax==axs[0] else 'coolwarm',vmin=None if ax==axs[0] else -.8,vmax=None if ax==axs[0] else .8)
        fig.colorbar(s,ax=ax,shrink=.7)
        ax.set_aspect('equal');ax.set_xlim(-34,34);ax.set_ylim(-32,32)
        ax.set_xticks(np.arange(-30,31,5));ax.set_yticks(np.arange(-30,31,5));ax.grid(alpha=.3);ax.set_title(name+' - '+title)
    fig.savefig(OUT/(name+'_diagnostics.png'),dpi=130);plt.close(fig)
    print(name,'height percentiles',np.percentile(v[:,2],[0,10,25,50,75,90,100]),'curvature',np.percentile(curv,[1,5,50,95,99]))

if __name__=='__main__':
    for name in ['upper','lower']:diagnostics(name)
