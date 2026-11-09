"""
Procedural Dental Anatomy Library Generator for DentIA.
Executed with Blender 5.1 CLI:
  blender.exe --background --factory-startup --python build_dental_library.py

Generates realistic 3D anatomical models (crown & root separated) for:
- incisivo_sup (Central/Lateral Upper Incisor)
- incisivo_inf (Lower Incisor)
- canino_sup (Upper Canine / Cusp)
- canino_inf (Lower Canine)
- premolar_sup (Upper Premolar / Bicuspid)
- premolar_inf (Lower Premolar)
- molar_inf (Lower Molar / 2 roots, 5 cusps)
- molar_sup (Upper Molar / 3 roots, 4 cusps)

Output directories:
- assets/dental/library/*.glb
- src/main/resources/static/models/dental/*.glb
"""

import math
import sys
from pathlib import Path
import bpy
import bmesh
from mathutils import Vector

MM = 0.001
TAU = math.tau

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
STATIC_MODELS_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "static" / "models" / "dental"
LIBRARY_DIR = SCRIPT_DIR / "library"

STATIC_MODELS_DIR.mkdir(parents=True, exist_ok=True)
LIBRARY_DIR.mkdir(parents=True, exist_ok=True)


def create_material(name, color, roughness=0.25, is_enamel=True):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get('Principled BSDF')
    if bsdf:
        bsdf.inputs['Base Color'].default_value = (*color, 1.0)
        bsdf.inputs['Roughness'].default_value = roughness
        if is_enamel and 'Subsurface Weight' in bsdf.inputs:
            bsdf.inputs['Subsurface Weight'].default_value = 0.04
            bsdf.inputs['Subsurface Radius'].default_value = (0.001, 0.0006, 0.0004)
    return mat


def make_mesh_obj(name, verts, faces, material, collection):
    mesh = bpy.data.meshes.new(name + "_mesh")
    mesh.from_pydata([tuple(c * MM for c in v) for v in verts], [], faces)
    mesh.update()

    bm = bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.recalc_face_normals(bm, faces=list(bm.faces))
    bm.to_mesh(mesh)
    bm.free()

    obj = bpy.data.objects.new(name, mesh)
    collection.objects.link(obj)
    obj.data.materials.append(material)
    for p in obj.data.polygons:
        p.use_smooth = True
    return obj


def create_loft(rings, is_closed_top=True, is_closed_bottom=True):
    """
    rings: list of lists of (x,y,z) tuples, each of same length N.
    """
    verts = []
    faces = []
    num_rings = len(rings)
    n = len(rings[0])

    for ring in rings:
        verts.extend(ring)

    for j in range(num_rings - 1):
        r0 = j * n
        r1 = (j + 1) * n
        for i in range(n):
            ni = (i + 1) % n
            faces.append((r0 + i, r0 + ni, r1 + ni, r1 + i))

    if is_closed_top:
        top_center = (0.0, 0.0, rings[-1][0][2])
        idx_tc = len(verts)
        verts.append(top_center)
        last_r = (num_rings - 1) * n
        for i in range(n):
            ni = (i + 1) % n
            faces.append((last_r + i, last_r + ni, idx_tc))

    if is_closed_bottom:
        bot_center = (0.0, 0.0, rings[0][0][2])
        idx_bc = len(verts)
        verts.append(bot_center)
        for i in range(n):
            ni = (i + 1) % n
            faces.append((i, idx_bc, ni))

    return verts, faces


# ==============================================================================
# ANATOMICAL TOOTH BUILDERS
# ==============================================================================

def build_incisor_upper(collection, enamel_mat, root_mat):
    # Spade/wedge shape crown
    n = 48
    crown_levels = 18
    crown_rings = []
    for j in range(crown_levels + 1):
        t = j / crown_levels
        z = 9.5 * t
        rx = 3.6 + 0.9 * math.sin(t * math.pi * 0.85)
        ry = (2.6 * (1.0 - 0.72 * t**1.3)) + 0.25
        ring = []
        for i in range(n):
            theta = TAU * i / n
            x = rx * math.cos(theta)
            y = ry * math.sin(theta)
            # Lingual concavity (cingulum at bottom, fossa in middle)
            if math.sin(theta) > 0.1:
                fossa = math.sin(theta) * math.sin(t * math.pi) * 0.55
                y -= fossa
            # Labial slight curvature
            if math.sin(theta) < -0.1:
                y += 0.25 * math.sin(t * math.pi * 0.6)
            ring.append((x, y, z))
        crown_rings.append(ring)

    cv, cf = create_loft(crown_rings, is_closed_top=True, is_closed_bottom=True)
    crown_obj = make_mesh_obj("Corona", cv, cf, enamel_mat, collection)
    crown_obj["part"] = "crown"

    # Single tapering conical root
    root_levels = 22
    root_rings = []
    for j in range(root_levels + 1):
        t = j / root_levels
        z = -12.5 * t
        scale = (1.0 - t)**0.62 * 0.95 + 0.05
        rx = 3.2 * scale
        ry = 3.4 * scale
        # Slight distal apical curvature
        cx = 0.5 * (t**2)
        cy = -0.3 * (t**2)
        ring = []
        for i in range(n):
            theta = TAU * i / n
            ring.append((cx + rx * math.cos(theta), cy + ry * math.sin(theta), z))
        root_rings.append(ring)

    rv, rf = create_loft(root_rings, is_closed_top=True, is_closed_bottom=True)
    root_obj = make_mesh_obj("Raices", rv, rf, root_mat, collection)
    root_obj["part"] = "roots"

    return crown_obj, root_obj


def build_incisor_lower(collection, enamel_mat, root_mat):
    # Smallest, chisel-shaped crown
    n = 40
    crown_levels = 16
    crown_rings = []
    for j in range(crown_levels + 1):
        t = j / crown_levels
        z = 8.5 * t
        rx = 2.4 + 0.5 * math.sin(t * math.pi * 0.8)
        ry = (2.2 * (1.0 - 0.70 * t**1.3)) + 0.22
        ring = []
        for i in range(n):
            theta = TAU * i / n
            ring.append((rx * math.cos(theta), ry * math.sin(theta), z))
        crown_rings.append(ring)

    cv, cf = create_loft(crown_rings, is_closed_top=True, is_closed_bottom=True)
    crown_obj = make_mesh_obj("Corona", cv, cf, enamel_mat, collection)
    crown_obj["part"] = "crown"

    # Slender flattened root
    root_levels = 20
    root_rings = []
    for j in range(root_levels + 1):
        t = j / root_levels
        z = -11.5 * t
        scale = (1.0 - t)**0.65 * 0.94 + 0.06
        rx = 2.0 * scale
        ry = 3.0 * scale
        ring = []
        for i in range(n):
            theta = TAU * i / n
            ring.append((rx * math.cos(theta), ry * math.sin(theta), z))
        root_rings.append(ring)

    rv, rf = create_loft(root_rings, is_closed_top=True, is_closed_bottom=True)
    root_obj = make_mesh_obj("Raices", rv, rf, root_mat, collection)
    root_obj["part"] = "roots"

    return crown_obj, root_obj


def build_canine(upper, collection, enamel_mat, root_mat):
    # Prominent cusp, diamond/pentagonal profile
    n = 48
    crown_levels = 20
    crown_rings = []
    height = 10.5 if upper else 9.8
    for j in range(crown_levels + 1):
        t = j / crown_levels
        z = height * t
        rx = (3.6 if upper else 3.2) * (1.0 - 0.78 * t**1.5) + 0.2
        ry = (3.8 if upper else 3.5) * (1.0 - 0.82 * t**1.6) + 0.2
        ring = []
        for i in range(n):
            theta = TAU * i / n
            # Labial ridge accentuation
            ridge = 1.0 + 0.15 * max(0.0, -math.sin(theta)) * (1.0 - t)
            ring.append((rx * math.cos(theta) * ridge, ry * math.sin(theta), z))
        crown_rings.append(ring)

    cv, cf = create_loft(crown_rings, is_closed_top=True, is_closed_bottom=True)
    crown_obj = make_mesh_obj("Corona", cv, cf, enamel_mat, collection)
    crown_obj["part"] = "crown"

    # Long, robust root
    root_levels = 24
    root_rings = []
    root_len = 16.0 if upper else 14.5
    for j in range(root_levels + 1):
        t = j / root_levels
        z = -root_len * t
        scale = (1.0 - t)**0.60 * 0.95 + 0.05
        rx = (3.4 if upper else 3.0) * scale
        ry = (3.8 if upper else 3.4) * scale
        cx = 0.6 * (t**2)  # slight distal apical bend
        ring = []
        for i in range(n):
            theta = TAU * i / n
            ring.append((cx + rx * math.cos(theta), ry * math.sin(theta), z))
        root_rings.append(ring)

    rv, rf = create_loft(root_rings, is_closed_top=True, is_closed_bottom=True)
    root_obj = make_mesh_obj("Raices", rv, rf, root_mat, collection)
    root_obj["part"] = "roots"

    return crown_obj, root_obj


def build_premolar(upper, collection, enamel_mat, root_mat):
    # 2 cusps (buccal & lingual), central groove
    n = 48
    crown_levels = 18
    crown_rings = []
    for j in range(crown_levels + 1):
        t = j / crown_levels
        z = 7.8 * t
        rx = 3.6 + 0.4 * math.sin(t * math.pi)
        ry = 4.2 + 0.5 * math.sin(t * math.pi)
        ring = []
        for i in range(n):
            theta = TAU * i / n
            # Indent occlusal table between buccal and lingual
            x = rx * math.cos(theta)
            y = ry * math.sin(theta)
            ring.append((x, y, z))
        crown_rings.append(ring)

    # Occlusal cusp addition
    verts, faces = create_loft(crown_rings, is_closed_top=False, is_closed_bottom=True)
    # Add occlusal table with 2 peaks
    top_verts = []
    top_start = len(verts)
    nx, ny = 7, 7
    for ix in range(nx):
        for iy in range(ny):
            px = -3.2 + (6.4 / (nx - 1)) * ix
            py = -3.8 + (7.6 / (ny - 1)) * iy
            # Two cusps: buccal (y < 0) and lingual (y > 0)
            b_cusp = 1.6 * math.exp(-(((px)/1.8)**2 + ((py + 2.0)/1.5)**2))
            l_cusp = 1.3 * math.exp(-(((px)/1.7)**2 + ((py - 2.0)/1.5)**2))
            pz = 7.8 + b_cusp + l_cusp - 0.3 * math.exp(-(py/0.9)**2)
            top_verts.append((px, py, pz))

    verts.extend(top_verts)
    # Connect grid
    for ix in range(nx - 1):
        for iy in range(ny - 1):
            p0 = top_start + ix * ny + iy
            p1 = p0 + 1
            p2 = top_start + (ix + 1) * ny + iy + 1
            p3 = top_start + (ix + 1) * ny + iy
            faces.append((p0, p1, p2, p3))

    # Stitch top rim to top_start
    rim_start = (crown_levels) * n
    for i in range(n):
        ni = (i + 1) % n
        # map to closest border vertex
        faces.append((rim_start + i, rim_start + ni, top_start + (i % (nx*ny))))

    crown_obj = make_mesh_obj("Corona", verts, faces, enamel_mat, collection)
    crown_obj["part"] = "crown"

    # Root: bifurcated or single grooved root
    root_levels = 22
    root_rings = []
    for j in range(root_levels + 1):
        t = j / root_levels
        z = -13.0 * t
        scale = (1.0 - t)**0.62 * 0.94 + 0.06
        rx = 2.8 * scale
        ry = 3.6 * scale
        ring = []
        for i in range(n):
            theta = TAU * i / n
            # Groove in mesial/distal
            groove = 1.0 - 0.18 * (math.cos(theta)**4) * (1.0 - t)
            ring.append((rx * math.cos(theta) * groove, ry * math.sin(theta), z))
        root_rings.append(ring)

    rv, rf = create_loft(root_rings, is_closed_top=True, is_closed_bottom=True)
    root_obj = make_mesh_obj("Raices", rv, rf, root_mat, collection)
    root_obj["part"] = "roots"

    return crown_obj, root_obj


def build_molar_lower(collection, enamel_mat, root_mat):
    # Lower molar: 5 cusps, 2 curved mesial/distal roots
    n = 48
    crown_levels = 18
    crown_rings = []
    for j in range(crown_levels + 1):
        t = j / crown_levels
        z = 7.5 * t
        rx = 5.2 + 0.4 * math.sin(t * math.pi)
        ry = 4.6 + 0.4 * math.sin(t * math.pi)
        ring = []
        for i in range(n):
            theta = TAU * i / n
            ring.append((rx * math.cos(theta), ry * math.sin(theta), z))
        crown_rings.append(ring)

    verts, faces = create_loft(crown_rings, is_closed_top=False, is_closed_bottom=True)
    top_start = len(verts)
    nx, ny = 9, 8
    top_verts = []
    for ix in range(nx):
        for iy in range(ny):
            px = -4.5 + (9.0 / (nx - 1)) * ix
            py = -3.8 + (7.6 / (ny - 1)) * iy
            # 5 cusps: MB, DB, Distal, ML, DL
            c_mb = 1.6 * math.exp(-(((px + 2.2)/1.6)**2 + ((py + 2.0)/1.4)**2))
            c_db = 1.4 * math.exp(-(((px - 0.8)/1.5)**2 + ((py + 2.0)/1.4)**2))
            c_d  = 1.1 * math.exp(-(((px - 3.4)/1.2)**2 + ((py + 1.2)/1.2)**2))
            c_ml = 1.7 * math.exp(-(((px + 1.8)/1.6)**2 + ((py - 2.0)/1.5)**2))
            c_dl = 1.5 * math.exp(-(((px - 1.6)/1.6)**2 + ((py - 2.0)/1.5)**2))
            pz = 7.5 + c_mb + c_db + c_d + c_ml + c_dl - 0.4 * math.exp(-((py/0.8)**2))
            top_verts.append((px, py, pz))

    verts.extend(top_verts)
    for ix in range(nx - 1):
        for iy in range(ny - 1):
            p0 = top_start + ix * ny + iy
            p1 = p0 + 1
            p2 = top_start + (ix + 1) * ny + iy + 1
            p3 = top_start + (ix + 1) * ny + iy
            faces.append((p0, p1, p2, p3))

    rim_start = crown_levels * n
    for i in range(n):
        ni = (i + 1) % n
        faces.append((rim_start + i, rim_start + ni, top_start + (i % (nx*ny))))

    crown_obj = make_mesh_obj("Corona", verts, faces, enamel_mat, collection)
    crown_obj["part"] = "crown"

    # Roots: 2 roots (Mesial and Distal), separate branches joined
    # Trunk:
    trunk_rings = []
    for j in range(7):
        t = j / 6
        z = -2.5 * t
        rx = 4.8 * (1.0 - 0.15 * t)
        ry = 4.2 * (1.0 - 0.15 * t)
        ring = [(rx * math.cos(TAU*i/n), ry * math.sin(TAU*i/n), z) for i in range(n)]
        trunk_rings.append(ring)
    tv, tf = create_loft(trunk_rings, is_closed_top=True, is_closed_bottom=False)
    trunk = make_mesh_obj("Trunk", tv, tf, root_mat, collection)

    def branch(cx_start, cx_end, name):
        b_rings = []
        bn = 24
        for j in range(16):
            t = j / 15
            z = -2.5 - 11.5 * t
            cx = cx_start * (1 - t) + cx_end * t
            rx = 2.1 * (1 - t)**0.6 + 0.15
            ry = 3.2 * (1 - t)**0.6 + 0.15
            ring = [(cx + rx * math.cos(TAU*i/bn), ry * math.sin(TAU*i/bn), z) for i in range(bn)]
            b_rings.append(ring)
        bv, bf = create_loft(b_rings, is_closed_top=True, is_closed_bottom=True)
        return make_mesh_obj(name, bv, bf, root_mat, collection)

    r_mesial = branch(2.2, 3.8, "R_Mesial")
    r_distal = branch(-2.2, -3.2, "R_Distal")

    bpy.ops.object.select_all(action='DESELECT')
    for o in [trunk, r_mesial, r_distal]:
        o.select_set(True)
    bpy.context.view_layer.objects.active = trunk
    bpy.ops.object.join()
    trunk.name = "Raices"
    trunk["part"] = "roots"

    return crown_obj, trunk


# ==============================================================================
# MAIN EXPORT PROCESSOR
# ==============================================================================

def export_tooth_type(type_key, builder_fn, *builder_args):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.unit_settings.system = 'METRIC'
    scene.unit_settings.scale_length = 1.0

    col = bpy.data.collections.new(f"Tooth_{type_key}")
    scene.collection.children.link(col)

    enamel_mat = create_material("Esmalte", (0.92, 0.88, 0.77), roughness=0.22, is_enamel=True)
    root_mat = create_material("Raiz", (0.72, 0.60, 0.44), roughness=0.42, is_enamel=False)

    crown, roots = builder_fn(col, enamel_mat, root_mat, *builder_args)

    parent = bpy.data.objects.new(f"Diente_{type_key}", None)
    col.objects.link(parent)
    crown.parent = parent
    roots.parent = parent
    parent["tooth_type"] = type_key

    # Apply decimation LOD for crisp web rendering (< 80-120 KB)
    for obj, ratio in [(crown, 0.28), (roots, 0.28)]:
        mod = obj.modifiers.new("DecimateWeb", "DECIMATE")
        mod.ratio = ratio
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.modifier_apply(modifier=mod.name)
        obj.data.validate(clean_customdata=True)

    # Select parent, crown, roots
    bpy.ops.object.select_all(action='DESELECT')
    parent.select_set(True)
    crown.select_set(True)
    roots.select_set(True)
    bpy.context.view_layer.objects.active = crown

    # Export to both library and static models
    lib_path = LIBRARY_DIR / f"{type_key}.glb"
    static_path = STATIC_MODELS_DIR / f"{type_key}.glb"

    bpy.ops.export_scene.gltf(
        filepath=str(lib_path),
        export_format='GLB',
        use_selection=True,
        export_extras=True,
        export_apply=True
    )

    import shutil
    shutil.copy2(str(lib_path), str(static_path))
    size_kb = static_path.stat().st_size / 1024
    print(f"GENERATED: {type_key} -> {static_path.name} ({size_kb:.1f} KB)", flush=True)


def main():
    print("=== DENTIA: GENERANDO BIBLIOTECA DENTAL 3D ANATOMICA ===", flush=True)

    items = [
        ("incisivo_sup", lambda col, em, rm: build_incisor_upper(col, em, rm)),
        ("incisivo_inf", lambda col, em, rm: build_incisor_lower(col, em, rm)),
        ("canino_sup", lambda col, em, rm: build_canine(True, col, em, rm)),
        ("canino_inf", lambda col, em, rm: build_canine(False, col, em, rm)),
        ("premolar_sup", lambda col, em, rm: build_premolar(True, col, em, rm)),
        ("premolar_inf", lambda col, em, rm: build_premolar(False, col, em, rm)),
    for type_key, builder in items:
        export_tooth_type(type_key, builder)

    # Sincronizar molar_sup desde el prototipo FDI 16
    proto_molar = SCRIPT_DIR / "prototype-16" / "molar_16_web.glb"
    if proto_molar.exists():
        import shutil
        shutil.copy2(str(proto_molar), str(LIBRARY_DIR / "molar_sup.glb"))
        shutil.copy2(str(proto_molar), str(STATIC_MODELS_DIR / "molar_sup.glb"))
        print(f"SYNCED: molar_sup -> molar_sup.glb ({proto_molar.stat().st_size / 1024:.1f} KB)", flush=True)

    print("=== BIBLIOTECA DENTAL GENERADA CON EXITO ===", flush=True)


if __name__ == "__main__":
    main()
