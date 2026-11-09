"""Check the exported GLBs by reading their metadata and importing into Blender."""
import json
import math
from pathlib import Path
import struct

import bpy
from mathutils import Vector

OUT=Path(__file__).resolve().parent
reports=[]
for filename in ['molar_16.glb','molar_16_web.glb']:
    path=OUT/filename
    raw=path.read_bytes()
    magic,version,total=struct.unpack_from('<4sII',raw,0)
    assert magic==b'glTF' and version==2 and total==len(raw)
    length,kind=struct.unpack_from('<I4s',raw,12)
    assert kind==b'JSON'
    meta=json.loads(raw[20:20+length])
    assert len(meta['meshes'])==2
    assert any(n.get('extras',{}).get('fdi')=='16' and 'children' in n for n in meta['nodes'])
    assert not meta.get('cameras')
    assert not meta.get('images'), 'No external texture assets expected'
    triangles=sum(meta['accessors'][p['indices']]['count']//3
                  for m in meta['meshes'] for p in m['primitives'])
    assert all(p.get('mode',4)==4 for m in meta['meshes'] for p in m['primitives'])
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(path))
    meshes=[o for o in bpy.context.scene.objects if o.type=='MESH']
    assert len(meshes)==2
    assert {o.get('part') for o in meshes}=={'crown','roots'}
    assert all(o.parent and o.parent.get('fdi')=='16' for o in meshes)
    points=[o.matrix_world @ Vector(corner) for o in meshes for corner in o.bound_box]
    dims=[(max(p[i] for p in points)-min(p[i] for p in points))*1000 for i in range(3)]
    assert all(math.isfinite(v) for v in dims)
    assert 18 < max(dims) < 24, dims
    assert all(len(o.data.materials)==1 for o in meshes)
    if filename.endswith('_web.glb'):
        assert triangles<15000
        assert len(raw)<400000
    reports.append({'file':filename,'size_bytes':len(raw),'triangles':triangles,
                    'dimensions_mm':[round(v,3) for v in dims],
                    'mesh_parts':[o.get('part') for o in meshes],
                    'import_roundtrip':'OK','metadata':'FDI 16, original procedural geometry'})
(OUT/'export_validation.json').write_text(json.dumps(reports,indent=2),encoding='utf-8')
print('DENTIA_EXPORT_VALIDATION_OK '+json.dumps(reports))
