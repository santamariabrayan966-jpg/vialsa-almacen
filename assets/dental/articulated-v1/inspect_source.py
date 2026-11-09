import bpy, bmesh, json
import numpy as np
from pathlib import Path

OUT=Path(__file__).resolve().parent

def groups(bm):
    bm.verts.ensure_lookup_table()
    seen=set(); result=[]
    for v in bm.verts:
        if v in seen: continue
        stack=[v]; seen.add(v); vs=[]
        while stack:
            u=stack.pop(); vs.append(u)
            for edge in u.link_edges:
                n=edge.other_vert(u)
                if n not in seen: seen.add(n); stack.append(n)
        xyz=np.array([v.co[:] for v in vs])
        result.append({'n':len(vs),'center':xyz.mean(axis=0).tolist(),'min':xyz.min(axis=0).tolist(),'max':xyz.max(axis=0).tolist()})
    return sorted(result,key=lambda g:-g['n'])

report=[]
for name in ['cerrada_arcada_superior','cerrada_arcada_inferior']:
    obj=bpy.data.objects[name]
    coords=np.array([v.co[:] for v in obj.data.vertices])
    result={'object':name,'local_min':coords.min(axis=0).tolist(),'local_max':coords.max(axis=0).tolist(),
            'matrix':list(map(list,obj.matrix_world)),'color_attributes':list(obj.data.color_attributes.keys()),'uv_layers':list(obj.data.uv_layers.keys())}
    for dist in [0.000001,0.00001,0.0001]:
        bm=bmesh.new(); bm.from_mesh(obj.data)
        bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=dist)
        result[str(dist)]={'vertices':len(bm.verts),'groups':groups(bm)}
        bm.free()
    report.append(result)
(OUT/'source_inspection.json').write_text(json.dumps(report,indent=2))
for r in report:
    print(r['object'],r['local_min'],r['local_max'],r['color_attributes'])
    for dist in ['1e-06','1e-05','0.0001']:
        print(dist,r[dist]['vertices'],len(r[dist]['groups']),r[dist]['groups'][:8])
