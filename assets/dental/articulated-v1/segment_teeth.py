import numpy as np
from scipy.sparse import coo_matrix
from scipy.spatial import cKDTree
import json, time
from pathlib import Path

OUT = Path(__file__).resolve().parent
START = time.time()

FDI_UPPER = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28]
FDI_LOWER = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38]

def elapsed():
    return f"{time.time()-START:.1f}s"

def build_adjacency(v, f):
    a = np.concatenate([f[:,0], f[:,1], f[:,2]])
    b = np.concatenate([f[:,1], f[:,2], f[:,0]])
    N = len(v)
    adj = coo_matrix((np.ones(2*len(a)), (np.r_[a,b], np.r_[b,a])), shape=(N,N)).tocsr()
    return adj, a, b

def laplacian_curvature(v, n, adj, a, b):
    degree = np.asarray(adj.sum(axis=1)).ravel()
    degree = np.maximum(degree, 1)
    lap = (adj @ v) / degree[:,None] - v
    curv = np.sum(lap * n, axis=1)
    return np.nan_to_num(curv)

def segment_arch(name, fdi_codes):
    print(f"\n{'='*55}")
    print(f"  Arcada: {name.upper()}")
    print(f"{'='*55}")

    data = np.load(OUT / f"{name}.npz")
    v, f, n = data['vertices'], data['faces'], data['normals']
    print(f"  [{elapsed()}] {len(v):,} verts, {len(f):,} faces")

    adj, a, b = build_adjacency(v, f)
    print(f"  [{elapsed()}] Grafo construido")

    curv = laplacian_curvature(v, n, adj, a, b)
    print(f"  [{elapsed()}] Curvatura calculada  percentiles=[{np.percentile(curv,[5,50,95]).round(3)}]")

    # Cara oclusal: normales apuntando hacia la mordida
    facing = -1 if name == 'upper' else 1
    oclusal_mask = (n[:,2] * facing > 0.2) & (curv * facing < -0.05)
    oclusal_idx = np.where(oclusal_mask)[0]
    print(f"  [{elapsed()}] Oclusal: {len(oclusal_idx):,} verts")

    # KMeans sobre proyeccion XY oclusal
    from sklearn.cluster import KMeans
    n_teeth = len(fdi_codes)
    xy = v[oclusal_idx][:, :2]
    print(f"  [{elapsed()}] KMeans {n_teeth} clusters...")
    km = KMeans(n_clusters=n_teeth, n_init=12, random_state=42, max_iter=400)
    km.fit(xy)

    # Ordenar centros de -X a +X (corresponde a FDI 18->11->21->28)
    order = np.argsort(km.cluster_centers_[:, 0])
    centers_xy = km.cluster_centers_[order]

    tooth_centers = {}
    for i, fdi in enumerate(fdi_codes):
        tooth_centers[fdi] = centers_xy[i].tolist()

    print(f"  [{elapsed()}] Centros encontrados:")
    for fdi, c in tooth_centers.items():
        print(f"    FDI {fdi:2d}  x={c[0]:+6.1f}  y={c[1]:+6.1f}")

    # Asignar cada vertice al diente mas cercano en XY
    centers_arr = np.array([tooth_centers[fdi] for fdi in fdi_codes])
    tree = cKDTree(centers_arr)
    _, labels = tree.query(v[:, :2], workers=-1)

    # Guardar etiquetas
    labels_arr = labels.astype(np.int16)
    np.save(OUT / f"{name}_labels.npy", labels_arr)

    # Guardar JSON con centros y conteos
    summary = {
        "arch": name,
        "fdi_codes": fdi_codes,
        "centers": {str(fdi): tooth_centers[fdi] for fdi in fdi_codes},
        "counts": {str(fdi_codes[i]): int((labels == i).sum()) for i in range(len(fdi_codes))}
    }
    (OUT / f"{name}_segments.json").write_text(json.dumps(summary, indent=2))

    print(f"  [{elapsed()}] Guardado {name}_labels.npy + {name}_segments.json")
    return tooth_centers

if __name__ == '__main__':
    segment_arch('upper', FDI_UPPER)
    segment_arch('lower', FDI_LOWER)
    print(f"\n[{elapsed()}] DONE - listos para Blender")
