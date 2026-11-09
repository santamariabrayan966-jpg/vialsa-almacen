"""
Blender script (ejecutar desde: blender --background source_normalized.blend --python rig_articulated.py)
Fase 3+4+5: Separar piezas, crear rig de apertura mandibular, exportar .glb
"""
import bpy, bmesh, json, math, sys
from pathlib import Path
import numpy as np

OUT = Path(__file__).resolve().parent
BLEND_FILE = OUT / "source_normalized.blend"

FDI_UPPER = [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28]
FDI_LOWER = [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38]

# Colores por tipo de diente (RGBA 0-1) para visualizacion en Blender
TOOTH_COLOR = {
    'molar':    (0.92, 0.88, 0.80, 1.0),
    'premolar': (0.90, 0.87, 0.82, 1.0),
    'canino':   (0.94, 0.90, 0.82, 1.0),
    'incisivo': (0.95, 0.93, 0.88, 1.0),
    'encia':    (0.85, 0.38, 0.46, 1.0),
}

def tipo_diente(fdi):
    d = fdi % 10
    if d in (8, 7, 6): return 'molar'
    if d in (5, 4):     return 'premolar'
    if d == 3:          return 'canino'
    return 'incisivo'

def make_material(name, color):
    mat = bpy.data.materials.new(name)
    # use_nodes is always on in Blender 5.x; set it anyway for safety
    if hasattr(mat, 'use_nodes'):
        mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get('Principled BSDF')
    if bsdf:
        bsdf.inputs['Base Color'].default_value = color
        bsdf.inputs['Roughness'].default_value = 0.22
        if 'Specular IOR Level' in bsdf.inputs:
            bsdf.inputs['Specular IOR Level'].default_value = 0.5
    return mat

def clear_scene():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for col in list(bpy.data.collections):
        bpy.data.collections.remove(col)

def load_arch_from_npz(name, fdi_codes, labels_path, segments_json_path):
    """Carga la malla NPZ, aplica las etiquetas de segmentacion,
    separa en sub-objetos por FDI y retorna la lista de objetos Blender."""
    data = np.load(OUT / f"{name}.npz")
    v_arr = data['vertices']   # (N,3)
    f_arr = data['faces']      # (M,3)

    labels = np.load(labels_path)   # (N,) int16 -> indice en fdi_codes
    seg_data = json.loads(segments_json_path.read_text())

    # Cargar centros para usarlos como pivots
    centers = seg_data['centers']

    objects = []
    for idx, fdi in enumerate(fdi_codes):
        tooth_mask = labels == idx
        vert_indices_global = np.where(tooth_mask)[0]

        if len(vert_indices_global) == 0:
            continue

        # Construir submalla: solo los vertices de este diente
        verts = v_arr[vert_indices_global]   # (Nv, 3)

        # Remap global face indices a local (solo caras cuyos 3 vertices esten en esta pieza)
        global_to_local = {int(gi): li for li, gi in enumerate(vert_indices_global)}
        local_faces = []
        for tri in f_arr:
            v0, v1, v2 = int(tri[0]), int(tri[1]), int(tri[2])
            if v0 in global_to_local and v1 in global_to_local and v2 in global_to_local:
                local_faces.append((global_to_local[v0], global_to_local[v1], global_to_local[v2]))

        if len(local_faces) == 0:
            continue

        # Crear mesh Blender
        mesh = bpy.data.meshes.new(f"mesh_fdi_{fdi}")
        mesh.from_pydata(verts.tolist(), [], local_faces)
        mesh.validate()
        mesh.update()

        # Smooth shading (Blender 5.x: use shade_smooth op or normals split)
        for poly in mesh.polygons:
            poly.use_smooth = True
        # auto_smooth removed in Blender 4+; use normals split modifier instead
        if hasattr(mesh, 'use_auto_smooth'):
            mesh.use_auto_smooth = True
            mesh.auto_smooth_angle = math.radians(30)

        # Material
        tipo = tipo_diente(fdi)
        mat_name = f"mat_{tipo}"
        if mat_name not in bpy.data.materials:
            mat = make_material(mat_name, TOOTH_COLOR[tipo])
        else:
            mat = bpy.data.materials[mat_name]
        mesh.materials.append(mat)

        # Objeto
        obj = bpy.data.objects.new(f"FDI_{fdi:02d}", mesh)
        obj['fdi'] = fdi
        obj['arch'] = name
        obj['tipo'] = tipo
        bpy.context.collection.objects.link(obj)

        # Poner el origen en el centro calculado por KMeans
        cx, cy = centers[str(fdi)]
        # El Z del origen = promedio Z de los vertices de la corona
        cz = float(verts[:, 2].mean())
        obj.location = (cx, cy, cz)

        objects.append(obj)
        print(f"  FDI {fdi:02d}  {tipo:8s}  {len(verts):5d} verts  {len(local_faces):5d} faces")

    return objects

def setup_jaw_armature(lower_objects, upper_objects):
    """Crea un armature con dos huesos: maxilar (fijo) y mandibula (movil).
    La mandibula rota alrededor del eje ATM (articulacion temporomandibular)."""
    bpy.ops.object.armature_add(enter_editmode=True, location=(0, 0, 0))
    arm_obj = bpy.context.active_object
    arm_obj.name = "Rig_Mandibula"
    arm = arm_obj.data
    arm.name = "ArmData_Mandibula"

    bm = arm.edit_bones

    # Borrar hueso default
    for b in list(bm):
        bm.remove(b)

    # Hueso raiz - maxilar (arcada superior, fija)
    maxilar = bm.new("Maxilar")
    maxilar.head = (0, 0, 5)
    maxilar.tail = (0, 0, 15)

    # Hueso mandibula (arcada inferior, rota para abrir la boca)
    # El pivote ATM esta aproximadamente en Y=+20mm, Z=0 (posterior)
    mandibula = bm.new("Mandibula")
    mandibula.head = (0, 22, 0)     # punto ATM
    mandibula.tail = (0, 0, -8)     # punta del menton
    mandibula.parent = maxilar
    mandibula.use_connect = False

    bpy.ops.object.editmode_toggle()

    # Pose bone constraint: limitar rotacion a 0-40 grados
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.posemode_toggle()
    pb = arm_obj.pose.bones['Mandibula']
    c = pb.constraints.new('LIMIT_ROTATION')
    c.use_limit_x = True
    c.min_x = 0.0
    c.max_x = math.radians(40)
    c.owner_space = 'LOCAL'
    bpy.ops.object.posemode_toggle()

    # Parear objetos inferiores al hueso Mandibula
    for obj in lower_objects:
        mod = obj.modifiers.new("Armature", 'ARMATURE')
        mod.object = arm_obj
        vg = obj.vertex_groups.new(name="Mandibula")
        vg.add(list(range(len(obj.data.vertices))), 1.0, 'REPLACE')
        obj.parent = arm_obj
        obj.parent_type = 'OBJECT'

    return arm_obj

def export_glb(objects, arm_obj, output_path, name):
    """Exporta la arcada como .glb con todos sus materiales."""
    bpy.ops.object.select_all(action='DESELECT')
    for obj in objects:
        obj.select_set(True)
    if arm_obj:
        arm_obj.select_set(True)

    export_kwargs = dict(
        filepath=str(output_path),
        use_selection=True,
        export_format='GLB',
        export_apply=True,
        export_materials='EXPORT',
    )
    # Optional flags that may vary by Blender version
    for key, val in [('export_animations', True), ('export_skins', True), ('export_normals', True)]:
        try:
            bpy.ops.export_scene.gltf(**{**export_kwargs, key: val})
            break
        except TypeError:
            export_kwargs.pop(key, None)
    else:
        bpy.ops.export_scene.gltf(**export_kwargs)
    print(f"  Exportado: {output_path.name}  ({output_path.stat().st_size/1024/1024:.2f} MB)")

# ═══════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════
def main():
    print("\n" + "="*60)
    print("  DentIA — Rig Articulado Dental")
    print("="*60)

    clear_scene()
    print("[Blender] Escena limpia")

    # Coleccion principal
    col = bpy.data.collections.new("DentIA_Arcadas")
    bpy.context.scene.collection.children.link(col)
    bpy.context.view_layer.active_layer_collection = \
        bpy.context.view_layer.layer_collection.children['DentIA_Arcadas']

    # Material de encia
    make_material("mat_encia", TOOTH_COLOR['encia'])

    print("\n[Fase 3] Cargando arcada SUPERIOR...")
    upper_objects = load_arch_from_npz(
        'upper', FDI_UPPER,
        OUT / 'upper_labels.npy',
        OUT / 'upper_segments.json'
    )
    print(f"  Total piezas superiores: {len(upper_objects)}")

    print("\n[Fase 3] Cargando arcada INFERIOR...")
    lower_objects = load_arch_from_npz(
        'lower', FDI_LOWER,
        OUT / 'lower_labels.npy',
        OUT / 'lower_segments.json'
    )
    print(f"  Total piezas inferiores: {len(lower_objects)}")

    print("\n[Fase 4] Creando rig de apertura mandibular...")
    arm_obj = setup_jaw_armature(lower_objects, upper_objects)
    print("  Armature 'Rig_Mandibula' creado")

    # Guardar el .blend de trabajo
    work_blend = OUT / "articulated_rig.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(work_blend))
    print(f"\n[Blend] Guardado: {work_blend.name}")

    print("\n[Fase 5] Exportando GLB...")
    static_dir = Path("src/main/resources/static/models/dental")

    export_glb(upper_objects, None, static_dir / "arcade_upper.glb", "upper")
    export_glb(lower_objects, arm_obj, static_dir / "arcade_lower.glb", "lower")

    print("\n" + "="*60)
    print("  DONE — Listo para Three.js")
    print("="*60)

main()
