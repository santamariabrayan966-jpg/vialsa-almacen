# export_from_blend.py
# Exporta Odontograma_cerrado.blend como GLB para DentIA / Three.js
import bpy, json
from pathlib import Path

OUT = Path("C:/Users/Brayan/Desktop/DentIA/src/main/resources/static/models/dental")
OUT.mkdir(parents=True, exist_ok=True)
META_PATH = Path("C:/Users/Brayan/Desktop/DentIA/assets/dental/articulated-v1/scene_objects.json")

# -- Listar escena
print("\n=== Objetos en la escena ===")
for obj in bpy.data.objects:
    bb = obj.bound_box
    xs = [v[0] for v in bb]; ys = [v[1] for v in bb]; zs = [v[2] for v in bb]
    cx = sum(xs)/8; cy = sum(ys)/8; cz = sum(zs)/8
    sx = max(xs)-min(xs); sy = max(ys)-min(ys); sz = max(zs)-min(zs)
    verts = len(obj.data.vertices) if obj.type == 'MESH' else 0
    print(f"  {obj.name:40s} type={obj.type} verts={verts} center=({cx:.1f},{cy:.1f},{cz:.1f}) size=({sx:.1f},{sy:.1f},{sz:.1f})")

# -- Material esmalte
def apply_enamel(obj):
    if obj.type != 'MESH':
        return
    if not obj.data.materials:
        mat = bpy.data.materials.new("mat_enamel")
        obj.data.materials.append(mat)
    for i, mat in enumerate(obj.data.materials):
        if mat is None:
            continue
        if not mat.node_tree:
            mat.use_nodes = True
        nodes = mat.node_tree.nodes
        bsdf = nodes.get('Principled BSDF')
        if bsdf is None:
            bsdf = nodes.new('ShaderNodeBsdfPrincipled')
        bsdf.inputs['Base Color'].default_value = (0.980, 0.965, 0.933, 1.0)
        bsdf.inputs['Roughness'].default_value = 0.22
        for spec_key in ('Specular IOR Level', 'Specular'):
            if spec_key in bsdf.inputs:
                bsdf.inputs[spec_key].default_value = 0.45
                break

# -- Exportar seleccion
def export_sel(filepath):
    bpy.ops.export_scene.gltf(
        filepath=str(filepath),
        use_selection=True,
        export_format='GLB',
        export_apply=True,
        export_materials='EXPORT',
    )
    mb = filepath.stat().st_size / 1024 / 1024
    print(f"  -> {filepath.name}  ({mb:.2f} MB)")

# Nombres posibles para cada arcada
UPPER_NAMES = ['cerrada_arcada_superior','arcada_superior','Arcada_Superior','Upper','upper']
LOWER_NAMES = ['cerrada_arcada_inferior','arcada_inferior','Arcada_Inferior','Lower','lower']

def select_group(names):
    bpy.ops.object.select_all(action='DESELECT')
    found = []
    for n in names:
        obj = bpy.data.objects.get(n)
        if obj:
            obj.select_set(True)
            bpy.context.view_layer.objects.active = obj
            apply_enamel(obj)
            found.append(n)
    return found

print("\n=== SUPERIOR ===")
up = select_group(UPPER_NAMES)
print("  Encontrados:", up)
if up:
    export_sel(OUT / "arcade_upper.glb")

print("\n=== INFERIOR ===")
lo = select_group(LOWER_NAMES)
print("  Encontrados:", lo)
if lo:
    export_sel(OUT / "arcade_lower.glb")

print("\n=== COMPLETO (toda la escena) ===")
bpy.ops.object.select_all(action='DESELECT')
all_names = UPPER_NAMES + LOWER_NAMES
all_found = []
for n in all_names:
    obj = bpy.data.objects.get(n)
    if obj:
        obj.select_set(True)
        apply_enamel(obj)
        all_found.append(n)
if all_found:
    export_sel(OUT / "dental_full.glb")

print("\n=== ESCENA COMPLETA (inspeccion) ===")
bpy.ops.object.select_all(action='SELECT')
for obj in bpy.data.objects:
    apply_enamel(obj)
export_sel(OUT / "dental_scene_all.glb")

# -- Metadata
meta = []
for obj in bpy.data.objects:
    if obj.type == 'MESH':
        bb = obj.bound_box
        xs = [v[0] for v in bb]; ys = [v[1] for v in bb]; zs = [v[2] for v in bb]
        meta.append({
            "name":   obj.name,
            "center": [round(sum(xs)/8,2), round(sum(ys)/8,2), round(sum(zs)/8,2)],
            "size":   [round(max(xs)-min(xs),2), round(max(ys)-min(ys),2), round(max(zs)-min(zs),2)],
            "verts":  len(obj.data.vertices),
            "faces":  len(obj.data.polygons),
        })
META_PATH.write_text(json.dumps(meta, indent=2))
print("Metadata:", META_PATH.name)
print("DONE")
