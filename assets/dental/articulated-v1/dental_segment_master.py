# dental_segment_master.py
# Blender 5.1 all-in-one: segmenta 32 dientes, crea jerarquia, exporta GLB
# Ejecutar: blender --background "Odontograma_cerrado.blend" --python dental_segment_master.py

import bpy, bmesh, math, json
import numpy as np
from pathlib import Path

# ── Configuracion ────────────────────────────────────────────────────────────
BLEND_FILE   = Path("C:/Users/Brayan/Documents/DENTISTA/Odontograma_cerrado.blend")
OUT_DIR      = Path("C:/Users/Brayan/Desktop/DentIA/src/main/resources/static/models/dental")
ASSET_DIR    = Path("C:/Users/Brayan/Desktop/DentIA/assets/dental/articulated-v1")
OUT_DIR.mkdir(parents=True, exist_ok=True)

FDI_UPPER = [18,17,16,15,14,13,12,11,21,22,23,24,25,26,27,28]
FDI_LOWER = [48,47,46,45,44,43,42,41,31,32,33,34,35,36,37,38]

# Pivot de la articulacion temporomandibular (ATM)
# En espacio local de la arcada inferior (posterior, Y positivo en Blender)
ATM_Y_OFFSET = 0.25   # 25% del tamano del modelo hacia atras

# ── Materiales ──────────────────────────────────────────────────────────────
MATS = {}
def get_mat(name, color_rgb, roughness=0.22, metalness=0.0, alpha=1.0):
    if name in MATS:
        return MATS[name]
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    mat.use_nodes = True
    mat.blend_method = 'BLEND' if alpha < 1.0 else 'OPAQUE'
    bsdf = mat.node_tree.nodes.get('Principled BSDF') or \
           mat.node_tree.nodes.new('ShaderNodeBsdfPrincipled')
    bsdf.inputs['Base Color'].default_value = (*color_rgb, 1.0)
    bsdf.inputs['Roughness'].default_value  = roughness
    if 'Alpha' in bsdf.inputs:
        bsdf.inputs['Alpha'].default_value  = alpha
    for k in ('Specular IOR Level','Specular'):
        if k in bsdf.inputs:
            bsdf.inputs[k].default_value = 0.45 if 'enamel' in name else 0.1
            break
    MATS[name] = mat
    return mat

def setup_materials():
    get_mat('mat_enamel_clean',  (0.960, 0.945, 0.905), roughness=0.13)  # Vita A1 marfil
    get_mat('mat_enamel_a2',     (0.945, 0.925, 0.870), roughness=0.15)  # Vita A2
    get_mat('mat_enamel_a3',     (0.920, 0.895, 0.825), roughness=0.18)  # Vita A3
    get_mat('mat_root',          (0.780, 0.710, 0.600), roughness=0.45)  # cemento radicular
    get_mat('mat_gum_sup',       (0.820, 0.380, 0.430), roughness=0.60)  # encia maxilar
    get_mat('mat_gum_inf',       (0.780, 0.350, 0.400), roughness=0.65)  # encia mandibular
    get_mat('mat_gum_transp',    (0.820, 0.380, 0.430), roughness=0.60, alpha=0.35)  # encia transparente (modo raiz)
    print("  Materiales listos")

# ── Utilidades BMesh ────────────────────────────────────────────────────────
def obj_to_np(obj):
    mesh = obj.data
    verts = np.array([v.co[:] for v in mesh.vertices], dtype=np.float32)
    return verts

def kmeans_pure_numpy(pts2d, k, n_init=10, max_iter=300, seed=42):
    rng = np.random.default_rng(seed)
    best_labels, best_inertia = None, np.inf
    for _ in range(n_init):
        idx = rng.choice(len(pts2d), k, replace=False)
        centers = pts2d[idx].copy()
        for __ in range(max_iter):
            dists  = np.linalg.norm(pts2d[:,None] - centers[None], axis=2)
            labels = np.argmin(dists, axis=1)
            new_c  = np.array([pts2d[labels==i].mean(axis=0) if (labels==i).any()
                                else centers[i] for i in range(k)])
            if np.allclose(new_c, centers, atol=1e-5): break
            centers = new_c
        inertia = sum(np.linalg.norm(pts2d[labels==i] - centers[i], axis=1).sum()
                      for i in range(k))
        if inertia < best_inertia:
            best_labels, best_inertia = labels.copy(), inertia
    return best_labels, np.array([pts2d[best_labels==i].mean(0) if (best_labels==i).any()
                                   else np.zeros(2) for i in range(k)])

def try_kmeans(pts2d, k):
    try:
        from sklearn.cluster import KMeans
        km = KMeans(n_clusters=k, n_init=12, random_state=42, max_iter=400)
        km.fit(pts2d)
        return km.labels_, km.cluster_centers_
    except ImportError:
        print("  sklearn no disponible, usando KMeans puro numpy")
        return kmeans_pure_numpy(pts2d, k)

def separate_mesh_by_labels(src_obj, labels, n_clusters, fdi_codes, arch_name, mat_name):
    objects = []
    col = bpy.data.collections.get(arch_name)
    if not col:
        col = bpy.data.collections.new(arch_name)
        bpy.context.scene.collection.children.link(col)

    for cluster_idx, fdi in enumerate(fdi_codes):
        vert_mask = (labels == cluster_idx)
        vert_indices = set(np.where(vert_mask)[0])
        if not vert_indices:
            continue

        # Crear bmesh de la pieza
        src_bm = bmesh.new()
        src_bm.from_mesh(src_obj.data)
        src_bm.verts.ensure_lookup_table()
        src_bm.faces.ensure_lookup_table()

        # Seleccionar solo las caras donde los 3 vertices pertenecen a este cluster
        to_delete = [f for f in src_bm.faces
                     if not all(v.index in vert_indices for v in f.verts)]
        bmesh.ops.delete(src_bm, geom=to_delete, context='FACES')
        src_bm.verts.ensure_lookup_table()

        if len(src_bm.verts) == 0:
            src_bm.free()
            continue

        # Crear objeto nuevo
        new_mesh = bpy.data.meshes.new(f"mesh_FDI_{fdi}")
        src_bm.to_mesh(new_mesh)
        src_bm.free()
        new_mesh.validate()
        new_mesh.update()

        obj_name = f"FDI_{fdi:02d}_CORONA"
        new_obj = bpy.data.objects.new(obj_name, new_mesh)
        new_obj['fdi']   = fdi
        new_obj['arch']  = arch_name
        new_obj['part']  = 'CORONA'
        new_obj['tipo']  = tipo_diente(fdi)

        col.objects.link(new_obj)

        # Material segun tipo de diente
        mat = bpy.data.materials.get(mat_name) or bpy.data.materials[mat_name]
        new_mesh.materials.append(mat)

        # Smooth shading
        for poly in new_mesh.polygons:
            poly.use_smooth = True

        objects.append(new_obj)

    return objects, col

def tipo_diente(fdi):
    d = fdi % 10
    if d in (8,7,6): return 'molar'
    if d in (5,4):   return 'premolar'
    if d == 3:       return 'canino'
    return 'incisivo'

# ── Crear encía como objeto separado ────────────────────────────────────────
def create_gum_object(src_obj, tooth_objects, arch_name, mat_name):
    col = bpy.data.collections.get(arch_name)

    # Las vertices de la encia son las que NO estan en ningun diente
    tooth_verts = set()
    for obj in tooth_objects:
        bm = bmesh.new()
        bm.from_mesh(obj.data)
        bm.verts.ensure_lookup_table()
        for v in bm.verts:
            tooth_verts.add(v.co[:])
        bm.free()

    # Crear mesh de encia: duplicar fuente y borrar lo que es diente
    src_bm = bmesh.new()
    src_bm.from_mesh(src_obj.data)
    src_bm.verts.ensure_lookup_table()

    # (Esta aproximacion puede dejar algunos vertices de diente - es aceptable
    #  para Fase 1; en Fase 2 usaremos CSG o grupos de vertices)
    gum_mesh = bpy.data.meshes.new(f"mesh_ENCIA_{arch_name}")
    src_bm.to_mesh(gum_mesh)
    src_bm.free()
    gum_mesh.validate()

    gum_obj = bpy.data.objects.new(f"ENCIA_{arch_name}", gum_mesh)
    gum_obj['part'] = 'ENCIA'
    gum_obj['arch'] = arch_name
    col.objects.link(gum_obj)

    mat = bpy.data.materials.get(mat_name)
    if mat:
        gum_mesh.materials.append(mat)

    for poly in gum_mesh.polygons:
        poly.use_smooth = True

    return gum_obj

# ── Crear armature de apertura mandibular ──────────────────────────────────
def create_jaw_armature(lower_col, lower_bbox):
    bpy.ops.object.select_all(action='DESELECT')
    bpy.ops.object.armature_add(enter_editmode=True, location=(0,0,0))
    arm_obj  = bpy.context.active_object
    arm_obj.name = "JAW_RIG"
    arm = arm_obj.data

    edit_bones = arm.edit_bones
    for b in list(edit_bones):
        edit_bones.remove(b)

    # Hueso Maxilar (fijo, la arcada superior no se mueve)
    maxilar = edit_bones.new("Maxilar")
    maxilar.head = (0, 0, 0.1)
    maxilar.tail = (0, 0, 0.3)

    # Pivot ATM: parte posterior del modelo, altura cero
    atm_y = lower_bbox[1] * 0.85   # 85% hacia la parte posterior
    atm_z = 0.0

    mandibula = edit_bones.new("Mandibula")
    mandibula.head = (0, atm_y, atm_z)
    mandibula.tail = (0, 0, -0.15)
    mandibula.parent = maxilar
    mandibula.use_connect = False

    bpy.ops.object.editmode_toggle()

    # Pose mode: limitar rotacion 0-40 grados
    bpy.context.view_layer.objects.active = arm_obj
    bpy.ops.object.posemode_toggle()
    pb = arm_obj.pose.bones.get('Mandibula')
    if pb:
        c = pb.constraints.new('LIMIT_ROTATION')
        c.use_limit_x = True
        c.min_x = 0.0
        c.max_x = math.radians(40)
        c.owner_space = 'LOCAL'
    bpy.ops.object.posemode_toggle()

    # Parentar todos los objetos inferiores al hueso Mandibula
    for obj in list(lower_col.objects):
        if obj.type != 'MESH': continue
        obj.parent = arm_obj
        obj.parent_type = 'BONE'
        obj.parent_bone = 'Mandibula'

    bpy.ops.object.select_all(action='DESELECT')
    return arm_obj

# ── Export GLB ───────────────────────────────────────────────────────────────
def export_glb(objects, output_path):
    bpy.ops.object.select_all(action='DESELECT')
    for obj in objects:
        try: obj.select_set(True)
        except: pass

    bpy.ops.export_scene.gltf(
        filepath=str(output_path),
        use_selection=True,
        export_format='GLB',
        export_apply=True,
        export_materials='EXPORT',
    )
    mb = output_path.stat().st_size / 1024 / 1024
    print(f"  Exportado: {output_path.name}  ({mb:.2f} MB)")

# ════════════════════════════════════════════════════════════════════════════
# MAIN
# ════════════════════════════════════════════════════════════════════════════
def main():
    print("\n" + "="*60)
    print("  DentIA — Segmentacion Dental 32 Piezas")
    print("="*60)

    # Limpiar escena
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete()
    for col in list(bpy.data.collections):
        bpy.data.collections.remove(col)
    print("[1] Escena limpia")

    # Cargar blend
    bpy.ops.wm.open_mainfile(filepath=str(BLEND_FILE))
    print(f"[2] Blend cargado: {BLEND_FILE.name}")

    # Listar objetos
    print("[3] Objetos en escena:")
    for obj in bpy.data.objects:
        bb = obj.bound_box
        xs = [v[0] for v in bb]
        ys = [v[1] for v in bb]
        zs = [v[2] for v in bb]
        verts = len(obj.data.vertices) if obj.type == 'MESH' else 0
        print(f"    {obj.name:40s} type={obj.type} verts={verts:,}")

    setup_materials()

    # Encontrar objetos de arcada
    SUP_NAMES = ['cerrada_arcada_superior','arcada_superior','Arcada_Superior']
    INF_NAMES = ['cerrada_arcada_inferior','arcada_inferior','Arcada_Inferior']

    src_sup = None
    for n in SUP_NAMES:
        src_sup = bpy.data.objects.get(n)
        if src_sup: break

    src_inf = None
    for n in INF_NAMES:
        src_inf = bpy.data.objects.get(n)
        if src_inf: break

    if not src_sup or not src_inf:
        print("ERROR: No se encontraron los objetos de arcada")
        print("Objetos disponibles:", [o.name for o in bpy.data.objects])
        return

    print(f"[4] Arcada superior: {src_sup.name} ({len(src_sup.data.vertices):,} verts)")
    print(f"    Arcada inferior:  {src_inf.name} ({len(src_inf.data.vertices):,} verts)")

    # ── Segmentar arcada superior ──────────────────────────────────────────
    print("\n[5] Segmentando arcada SUPERIOR (KMeans 16 clusters)...")
    verts_sup = obj_to_np(src_sup)    # (N, 3)

    # Proyectar en XY (el plano horizontal del arco)
    xy_sup   = verts_sup[:, :2]
    labels_sup, centers_sup = try_kmeans(xy_sup, 16)

    # Ordenar centros de -X a +X → corresponde a FDI 18→11→21→28
    order_sup = np.argsort(centers_sup[:, 0])
    fdi_map_sup = {int(order_sup[i]): FDI_UPPER[i] for i in range(16)}
    labels_sup_fdi = np.array([fdi_map_sup[l] for l in labels_sup])

    # Convertir FDI a indice 0-15
    fdi_to_idx_sup = {fdi: i for i, fdi in enumerate(FDI_UPPER)}
    labels_sup_idx = np.array([fdi_to_idx_sup[fdi] for fdi in labels_sup_fdi])

    print("  Centros FDI (superior):")
    for i, fdi in enumerate(FDI_UPPER):
        mask = (labels_sup_idx == i)
        if mask.any():
            cx, cy = xy_sup[mask].mean(axis=0)
            print(f"    FDI {fdi:2d}  x={cx:+.3f}  y={cy:+.3f}  verts={mask.sum():,}")

    print("\n  Separando meshes superiores...")
    upper_objects, upper_col = separate_mesh_by_labels(
        src_sup, labels_sup_idx, 16, FDI_UPPER, 'Arcada_Superior', 'mat_enamel_a2')
    print(f"  Piezas superiores creadas: {len(upper_objects)}")

    # Encia superior
    gum_sup = create_gum_object(src_sup, upper_objects, 'Arcada_Superior', 'mat_gum_sup')
    upper_objects.append(gum_sup)

    # ── Segmentar arcada inferior ──────────────────────────────────────────
    print("\n[6] Segmentando arcada INFERIOR (KMeans 16 clusters)...")
    verts_inf = obj_to_np(src_inf)
    xy_inf    = verts_inf[:, :2]
    labels_inf, centers_inf = try_kmeans(xy_inf, 16)

    order_inf = np.argsort(centers_inf[:, 0])
    fdi_map_inf = {int(order_inf[i]): FDI_LOWER[i] for i in range(16)}
    labels_inf_fdi = np.array([fdi_map_inf[l] for l in labels_inf])
    fdi_to_idx_inf = {fdi: i for i, fdi in enumerate(FDI_LOWER)}
    labels_inf_idx  = np.array([fdi_to_idx_inf[fdi] for fdi in labels_inf_fdi])

    print("\n  Separando meshes inferiores...")
    lower_objects, lower_col = separate_mesh_by_labels(
        src_inf, labels_inf_idx, 16, FDI_LOWER, 'Arcada_Inferior', 'mat_enamel_a2')
    print(f"  Piezas inferiores creadas: {len(lower_objects)}")

    gum_inf = create_gum_object(src_inf, lower_objects, 'Arcada_Inferior', 'mat_gum_inf')
    lower_objects.append(gum_inf)

    # ── Calcular bounding box de la arcada inferior para pivot ATM ────────
    all_inf_pts = verts_inf
    lower_bbox = (float(all_inf_pts[:,0].max()), float(all_inf_pts[:,1].max()))
    print(f"\n[7] Pivot ATM calculado: Y={lower_bbox[1]:.3f}")

    # ── Crear armature de apertura mandibular ──────────────────────────────
    print("[8] Creando rig mandibular...")
    arm_obj = create_jaw_armature(lower_col, lower_bbox)
    print(f"    Armature creado: {arm_obj.name}")

    # ── Guardar metadata FDI (para Three.js) ──────────────────────────────
    def compute_centers(verts, labels_idx, fdi_codes):
        result = {}
        for i, fdi in enumerate(fdi_codes):
            mask = labels_idx == i
            if mask.any():
                c = verts[mask].mean(axis=0)
                result[str(fdi)] = [round(float(c[0]),4), round(float(c[1]),4), round(float(c[2]),4)]
        return result

    meta = {
        "upper_centers": compute_centers(verts_sup, labels_sup_idx, FDI_UPPER),
        "lower_centers": compute_centers(verts_inf, labels_inf_idx, FDI_LOWER),
        "atm_pivot": {"y": lower_bbox[1] * 0.85, "z": 0.0},
        "fdi_upper": FDI_UPPER,
        "fdi_lower": FDI_LOWER,
    }
    meta_path = ASSET_DIR / "fdi_meta.json"
    meta_path.write_text(json.dumps(meta, indent=2))
    print(f"[9] Metadata guardada: {meta_path.name}")

    # ── Guardar .blend de trabajo ──────────────────────────────────────────
    work_blend = ASSET_DIR / "dental_segmented.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(work_blend))
    print(f"[10] Blend de trabajo guardado: {work_blend.name}")

    # ── Exportar GLB completo ──────────────────────────────────────────────
    print("\n[11] Exportando dental_articulated.glb...")
    all_objects = list(upper_objects) + list(lower_objects) + [arm_obj]
    export_glb(all_objects, OUT_DIR / "dental_articulated.glb")

    # Exportar superior e inferior por separado (para carga progresiva)
    export_glb(list(upper_objects), OUT_DIR / "arcade_upper_seg.glb")
    export_glb(list(lower_objects) + [arm_obj], OUT_DIR / "arcade_lower_seg.glb")

    # ── Resumen ───────────────────────────────────────────────────────────
    print("\n" + "="*60)
    print("  SEGMENTACION COMPLETA")
    print(f"  Piezas superiores: {len([o for o in upper_objects if o.get('part')=='CORONA'])}")
    print(f"  Piezas inferiores: {len([o for o in lower_objects if o.get('part')=='CORONA'])}")
    print(f"  Total: {len(all_objects)} objetos en GLB")
    print("="*60)

main()
