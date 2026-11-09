"""Original procedural molar for DentIA. Run with Blender 5.1 --python.

Coordinates are designed in millimetres and stored in metres for glTF.
Illustrative maxillary right first molar (FDI 16), not a patient scan.
No third-party mesh, texture, or patient image is used.
"""

import argparse
import json
import math
from pathlib import Path
import sys

import bpy
import bmesh
from mathutils import Vector

OUT = Path(__file__).resolve().parent
TAU = math.tau
MM = 0.001


def mat(name, color, roughness, subsurface=0.0, vertex_colors=False):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*color, 1)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get('Principled BSDF')
    bsdf.inputs['Base Color'].default_value = (*color, 1)
    bsdf.inputs['Roughness'].default_value = roughness
    bsdf.inputs['IOR'].default_value = 1.48
    bsdf.inputs['Subsurface Weight'].default_value = subsurface
    bsdf.inputs['Subsurface Radius'].default_value = (0.001, 0.0006, 0.0003)
    if vertex_colors:
        vcol = m.node_tree.nodes.new('ShaderNodeVertexColor')
        vcol.layer_name = 'DentalColor'
        m.node_tree.links.new(vcol.outputs['Color'], bsdf.inputs['Base Color'])
    return m


def mesh_object(name, verts, faces, collection, material):
    mesh = bpy.data.meshes.new(name + '_mesh')
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
    for face in obj.data.polygons:
        face.use_smooth = True
    return obj


def smoothstep(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def contour(theta):
    # Rounded rhomboid; modest distal taper, with soft buccal lobes.
    c, s = math.cos(theta), math.sin(theta)
    radius = ((abs(c) / 5.05) ** 3.1 + (abs(s) / 5.55) ** 3.1) ** (-1 / 3.1)
    x, y = radius * c, radius * s
    y *= 1 - 0.065 * (x / 5.05)
    x += 0.095 * y
    radial_lobe = 1 + 0.012 * math.cos(4 * theta + 0.3)
    return x * radial_lobe, y * radial_lobe


def segment_distance(x, y, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    t = max(0, min(1, ((x - a[0]) * dx + (y - a[1]) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - a[0] - t * dx, y - a[1] - t * dy)


FISSURES = [
    ((-3.25, -0.2), (-1.3, -0.55), 0.13, 0.28),
    ((-1.3, -0.55), (0.3, -0.35), 0.13, 0.31),
    ((0.3, -0.35), (1.35, -1.05), 0.12, 0.22),
    ((-0.45, -0.45), (-0.10, -2.25), 0.12, 0.24),
    ((-0.10, -2.25), (0.08, -4.25), 0.14, 0.21),
    ((1.3, 0.8), (2.55, 1.2), 0.11, 0.23),
    ((2.55, 1.2), (3.60, 0.6), 0.11, 0.18),
    ((1.35, 0.9), (0.85, 2.6), 0.12, 0.26),
    ((0.85, 2.6), (0.30, 4.2), 0.14, 0.20),
    ((-2.0, -0.4), (-3.0, -1.35), 0.10, 0.13),
    ((-2.0, -0.4), (-3.4, 0.9), 0.10, 0.11),
]


def crown_top(x, y):
    # Four broad cusps of intentionally different sizes and heights.
    cusps = [(-2.60, -2.50, 2.10, 1.70, 1.83),
             (2.40, -2.45, 1.85, 1.62, 1.70),
             (-2.15, 2.30, 2.55, 1.96, 1.95),
             (2.50, 2.70, 1.67, 1.54, 1.54)]
    z = 4.78
    for cx, cy, height, sx, sy in cusps:
        z += height * math.exp(-0.5 * (((x-cx)/sx)**2 + ((y-cy)/sy)**2))
    # Oblique ridge connects the mesiolingual and distobuccal cusps.
    ridge = segment_distance(x, y, (-1.6, 1.75), (1.8, -1.65))
    z += 0.38 * math.exp(-(ridge / 0.75)**2)
    z -= max(depth * math.exp(-(segment_distance(x, y, a, b)/width)**2)
             for a, b, width, depth in FISSURES)
    # Central and distal fossae; shallow, unpigmented developmental depressions.
    z -= 0.17 * math.exp(-(((x+0.6)/0.60)**2 + ((y+0.5)/0.60)**2))
    z -= 0.13 * math.exp(-(((x-2.5)/0.45)**2 + ((y-0.95)/0.45)**2))
    return z


def create_crown(collection, material):
    around, side_rings, top_rings = 192, 30, 66
    verts, faces = [], []
    # Cervical margin -> convex axial walls -> occlusal boundary.
    for j in range(side_rings + 1):
        t = j / side_rings
        radius = 0.785 + 0.245 * math.sin(t * math.pi / 2) + 0.060 * math.sin(t * math.pi) - 0.100 * t**8
        for i in range(around):
            theta = TAU * i / around
            bx, by = contour(theta)
            x, y = bx * radius, by * radius
            terminal = crown_top(bx * 0.93, by * 0.93)
            cervical_curve = 0.16 * math.cos(theta * 2 + 0.1)
            z = terminal * t + cervical_curve * (1-t)
            # Vertical developmental sulcus on the buccal crown face.
            buccal = smoothstep(0.0, 0.8, -math.sin(theta))
            y += 0.10 * math.exp(-((x-0.1)/0.50)**2) * buccal * smoothstep(0.3, 1.0, t)
            verts.append((x, y, z))
    for j in range(side_rings):
        for i in range(around):
            k = j * around + i
            kn = j * around + (i+1) % around
            faces.append((k, kn, kn + around, k + around))
    # Interior rings share the outermost side ring, giving a watertight crown.
    for j in range(1, top_rings):
        r = 0.93 * (1 - j / top_rings)
        for i in range(around):
            bx, by = contour(TAU * i / around)
            x, y = r * bx, r * by
            verts.append((x, y, crown_top(x, y)))
        prior = (side_rings + j - 1) * around
        current = prior + around
        for i in range(around):
            ni = (i+1) % around
            faces.append((prior+i, prior+ni, current+ni, current+i))
    center = len(verts)
    verts.append((0, 0, crown_top(0, 0)))
    last = (side_rings + top_rings-1)*around
    for i in range(around):
        faces.append((last+i, last+(i+1)%around, center))
    bottom = len(verts)
    verts.append((0, 0, -0.18))
    for i in range(around):
        faces.append((i, bottom, (i+1)%around))
    obj = mesh_object('FDI_16_Corona', verts, faces, collection, material)
    colors = obj.data.color_attributes.new(name='DentalColor', type='FLOAT_COLOR', domain='POINT')
    for vertex, col in zip(obj.data.vertices, colors.data):
        x, y, z = (v / MM for v in vertex.co)
        white = smoothstep(0, 5.6, z)
        cervical = (0.69, 0.555, 0.36)
        enamel = (0.92, 0.865, 0.705)
        col.color = tuple(cervical[k]*(1-white) + enamel[k]*white for k in range(3)) + (1,)
    obj['part'] = 'crown'
    return obj


def ellipsoid(name, center, radii, collection, material):
    verts, faces = [], []
    n, rings = 64, 32
    verts.append((center[0], center[1], center[2]+radii[2]))
    for j in range(1, rings):
        phi = math.pi*j/rings
        for i in range(n):
            a=TAU*i/n
            verts.append((center[0]+radii[0]*math.sin(phi)*math.cos(a),
                          center[1]+radii[1]*math.sin(phi)*math.sin(a),
                          center[2]+radii[2]*math.cos(phi)))
    for i in range(n): faces.append((0, 1+i, 1+(i+1)%n))
    for j in range(rings-2):
        for i in range(n):
            k=1+j*n+i; kn=1+j*n+(i+1)%n
            faces.append((k,k+n,kn+n,kn))
    bottom=len(verts); verts.append((center[0],center[1],center[2]-radii[2]))
    last=1+(rings-2)*n
    for i in range(n): faces.append((last+i,bottom,last+(i+1)%n))
    return mesh_object(name,verts,faces,collection,material)


def root_branch(name, origin, tip, widths, collection, material):
    verts, faces = [], []
    sides, rings = 64, 64
    for j in range(rings):
        t=j/rings
        radius = (1-t)**0.63 * (1+0.08*math.sin(math.pi*t))
        z=origin[2]*(1-t)+tip[2]*t
        cx=origin[0]*(1-t)+tip[0]*t + 0.30*math.sin(math.pi*t)
        cy=origin[1]*(1-t)+tip[1]*t + 0.15*math.sin(math.pi*t)
        for i in range(sides):
            theta=TAU*i/sides
            groove=1-0.095*(math.cos(2*theta)**12)*math.sin(math.pi*t)
            verts.append((cx+widths[0]*radius*math.cos(theta)*groove,
                          cy+widths[1]*radius*math.sin(theta)*groove,z))
    for j in range(rings-1):
        for i in range(sides):
            k=j*sides+i; kn=j*sides+(i+1)%sides
            faces.append((k,kn,kn+sides,k+sides))
    top=len(verts); verts.append(origin)
    bottom=len(verts); verts.append(tip)
    for i in range(sides):
        faces.append((i,top,(i+1)%sides))
        k=(rings-1)*sides
        faces.append((k+i,k+(i+1)%sides,bottom))
    return mesh_object(name,verts,faces,collection,material)


def create_roots(collection, material):
    # Taper downwards from the cervical margin; avoid a bulging collar below it.
    verts,faces=[],[]
    count,levels=96,32
    for j in range(levels):
        t=j/(levels-1)
        z=0.5-4.0*t
        radius=0.798*(1-0.62*t**1.25)
        for i in range(count):
            x,y=contour(TAU*i/count)
            verts.append((x*radius,y*radius,z))
    for j in range(levels-1):
        for i in range(count):
            k=j*count+i; kn=j*count+(i+1)%count
            faces.append((k,kn,kn+count,k+count))
    top=len(verts); verts.append((0,0,0.5))
    bottom=len(verts); verts.append((0,0,-3.5))
    for i in range(count):
        faces.append((i,top,(i+1)%count))
        k=(levels-1)*count
        faces.append((k+i,k+(i+1)%count,bottom))
    trunk=mesh_object('FDI_16_Raices',verts,faces,collection,material)
    parts=[trunk,
           root_branch('MB',(-2.1,-2.2,-1.0),(-3.6,-3.8,-12.4),(1.65,1.45),collection,material),
           root_branch('DB',(2.1,-2.2,-1.0),(3.9,-3.6,-11.6),(1.38,1.30),collection,material),
           root_branch('P',(0.0,2.25,-1.0),(0.45,4.65,-13.4),(1.80,1.85),collection,material)]
    bpy.ops.object.select_all(action='DESELECT')
    for obj in parts: obj.select_set(True)
    bpy.context.view_layer.objects.active=trunk
    bpy.ops.object.join()
    # Perform remeshing in millimetre-sized coordinates to avoid tiny-face
    # tolerances in Blender's voxel and decimation algorithms.
    for vertex in trunk.data.vertices: vertex.co /= MM
    # Union the root trunk and three branches; avoid interior intersecting shells.
    remesh=trunk.modifiers.new('Fusion radicular','REMESH')
    remesh.mode='VOXEL'; remesh.voxel_size=0.085
    remesh.use_smooth_shade=True
    bpy.ops.object.modifier_apply(modifier=remesh.name)
    smooth=trunk.modifiers.new('Alisado radicular','SMOOTH')
    smooth.factor=0.65; smooth.iterations=16
    bpy.ops.object.modifier_apply(modifier=smooth.name)
    decimate=trunk.modifiers.new('Optimizar malla','DECIMATE')
    decimate.ratio=0.23
    bpy.ops.object.modifier_apply(modifier=decimate.name)
    trunk.data.validate(clean_customdata=True)
    bm=bmesh.new(); bm.from_mesh(trunk.data)
    bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=0.00001)
    bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=0.000001)
    holes=[edge for edge in bm.edges if edge.is_boundary]
    if holes: bmesh.ops.holes_fill(bm,edges=holes,sides=0)
    bmesh.ops.triangulate(bm,faces=list(bm.faces))
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    bm.to_mesh(trunk.data); bm.free()
    for vertex in trunk.data.vertices: vertex.co *= MM
    trunk.data.update()
    for face in trunk.data.polygons: face.use_smooth=True
    trunk['part']='roots'
    trunk['root_count']=3
    return trunk


def light(name, position, energy, size, color, target, collection):
    data=bpy.data.lights.new(name,'AREA')
    data.energy=energy; data.shape='DISK'; data.size=size*MM; data.color=color
    obj=bpy.data.objects.new(name,data); collection.objects.link(obj)
    obj.location=Vector(position)*MM
    obj.rotation_euler=(Vector(target)*MM-obj.location).to_track_quat('-Z','Y').to_euler()
    obj.hide_set(True)
    return obj


def camera(name, position, target, scale, collection):
    data=bpy.data.cameras.new(name); data.type='ORTHO'; data.ortho_scale=scale*MM
    data.clip_start=0.1*MM; data.clip_end=2
    obj=bpy.data.objects.new(name,data); collection.objects.link(obj)
    obj.location=Vector(position)*MM
    obj.rotation_euler=(Vector(target)*MM-obj.location).to_track_quat('-Z','Y').to_euler()
    obj.hide_set(True)
    return obj


def inspect_mesh(obj):
    bm=bmesh.new(); bm.from_mesh(obj.data)
    boundary=sum(1 for edge in bm.edges if edge.is_boundary)
    nonmanifold=sum(1 for edge in bm.edges if not edge.is_manifold)
    volume=bm.calc_volume(signed=True)
    bm.free()
    obj.data.calc_loop_triangles()
    result={'name':obj.name,'vertices':len(obj.data.vertices),
            'triangles':len(obj.data.loop_triangles),'boundary_edges':boundary,
            'nonmanifold_edges':nonmanifold,'volume_mm3':round(volume/MM**3,3)}
    if nonmanifold or volume <= 0: raise RuntimeError('Malla no valida: '+str(result))
    return result


def build():
    # This runs in a fresh Blender process, leaving existing open work untouched.
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene=bpy.context.scene
    scene.name='DentIA | Molar 16 | Prototipo v01.1'
    scene.unit_settings.system='METRIC'; scene.unit_settings.length_unit='MILLIMETERS'
    scene.unit_settings.scale_length=1.0
    dental=bpy.data.collections.new('01 | Pieza dental FDI 16')
    studio=bpy.data.collections.new('02 | Estudio de presentacion')
    scene.collection.children.link(dental); scene.collection.children.link(studio)
    enamel=mat('Esmalte | Marfil natural',(0.9,0.84,0.70),0.27,0.025,True)
    rootmat=mat('Raiz | Cemento',(0.70,0.57,0.39),0.40,0.01)
    crown=create_crown(dental,enamel)
    roots=create_roots(dental,rootmat)
    parent=bpy.data.objects.new('FDI_16 | Primer molar superior derecho',None)
    dental.objects.link(parent)
    parent.empty_display_size=0.001
    parent['fdi']='16'; parent['dentition']='permanent'; parent['side']='right'
    parent['arch']='maxillary'; parent['tooth_type']='first_molar'
    parent['source']='Original procedural geometry for DentIA'
    parent['version']='0.1.1'; parent['approximate']=True
    parent['local_axes']='X mesiodistal, Y buccopalatal, Z root to crown; metres'
    for obj in (crown,roots):
        obj.parent=parent; obj['fdi']='16'
    # Root origin/rotation pivot stays at the cervical centre for web manipulation.
    floor_mat=mat('Fondo | Azul pizarra',(0.045,0.065,0.085),0.80)
    floor=mesh_object('Fondo estudio',[(-200,-200,-13.7),(200,-200,-13.7),
                                     (200,200,-13.7),(-200,200,-13.7)],[(0,1,2,3)],studio,floor_mat)
    floor.hide_set(True)
    light('Softbox principal',(-27,-30,37),0.035,30,(1.0,0.93,0.82),(0,0,-2),studio)
    light('Relleno frio',(28,-12,15),0.016,24,(0.80,0.9,1.0),(0,0,-2),studio)
    light('Luz de borde',(8,24,20),0.040,22,(0.80,0.92,1.0),(0,0,-2),studio)
    scene.world=bpy.data.worlds.new('Entorno estudio')
    scene.world.use_nodes=True
    bg=scene.world.node_tree.nodes.get('Background')
    bg.inputs['Color'].default_value=(0.32,0.38,0.48,1); bg.inputs['Strength'].default_value=0.28
    beauty=camera('Camara | Tres cuartos',(27,-43,24),(0,0,-2.2),30,studio)
    camera('Camara | Oclusal',(0,0,48),(0,0,0),16.0,studio)
    camera('Camara | Lateral',(-48,-14,5),(0,0,-2.6),28,studio)
    scene.camera=beauty
    scene.render.engine='CYCLES'; scene.cycles.samples=32; scene.cycles.use_denoising=True
    scene.render.threads_mode='FIXED'; scene.render.threads=8
    scene.render.resolution_x=1400; scene.render.resolution_y=1400
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='PNG'
    scene.view_settings.view_transform='AgX'
    # Pleasant editable workspace: front three-quarter view, no grid or rig clutter.
    for screen in bpy.data.screens:
        for area in screen.areas:
            if area.type=='VIEW_3D':
                space=area.spaces.active
                space.clip_start=0.0001; space.clip_end=10
                space.overlay.show_floor=False; space.overlay.show_axis_x=False; space.overlay.show_axis_y=False
                space.shading.type='SOLID'; space.shading.light='STUDIO'
                space.shading.color_type='MATERIAL'; space.shading.show_cavity=True
                space.shading.cavity_type='BOTH'
                space.shading.background_type='WORLD'
                space.region_3d.view_distance=0.043
                space.region_3d.view_location=(0,0,-0.002)
                space.region_3d.view_rotation=beauty.rotation_euler.to_quaternion()
                space.region_3d.view_perspective='ORTHO'
    bpy.ops.object.select_all(action='DESELECT')
    crown.select_set(True); roots.select_set(True); parent.select_set(True)
    bpy.context.view_layer.objects.active=crown
    checks=[inspect_mesh(crown),inspect_mesh(roots)]
    bpy.ops.export_scene.gltf(filepath=str(OUT/'molar_16.glb'),export_format='GLB',
                             use_selection=True,export_extras=True,export_animations=False,
                             export_cameras=False,export_lights=False,export_apply=True)
    # The web GLB uses temporary decimation; the editable .blend keeps all detail.
    web_modifiers=[]
    for obj,ratio in [(crown,0.17),(roots,0.20)]:
        modifier=obj.modifiers.new('LOD web temporal','DECIMATE'); modifier.ratio=ratio
        web_modifiers.append((obj,modifier))
    bpy.ops.export_scene.gltf(filepath=str(OUT/'molar_16_web.glb'),export_format='GLB',
                             use_selection=True,export_extras=True,export_animations=False,
                             export_cameras=False,export_lights=False,export_apply=True)
    for obj,modifier in web_modifiers: obj.modifiers.remove(modifier)
    report={'fdi':'16','version':'0.1.1','units':'metres','blender':bpy.app.version_string,
            'meshes':checks,'glb_bytes':(OUT/'molar_16.glb').stat().st_size,
            'web_glb_bytes':(OUT/'molar_16_web.glb').stat().st_size,
            'purpose':'Illustrative editable odontogram base. Not measured patient anatomy.'}
    (OUT/'validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    notes=bpy.data.texts.new('LEEME | DentIA')
    notes.write('DentIA - Pieza de prueba FDI 16\n\nGeometria original, modelada mediante build_molar.py.\nCorona y conjunto radicular separados. Cuatro cuspides principales y tres raices.\nUnidades: metros; visualizacion en milimetros. Origen en centro cervical.\nSeleccionar la corona o las raices desde la coleccion 01.\nLa coleccion 02 contiene camaras, luces y fondo solo para presentacion.\nModelo ilustrativo aproximado, pendiente de revision odontologica.\nNo deriva de una radiografia ni contiene datos de pacientes.\n')
    # Deselect for a clean preview; each object remains individually selectable.
    bpy.ops.object.select_all(action='DESELECT')
    scene.render.filepath=str(OUT/'molar_16_preview.png')
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'molar_16.blend'))
    print('DENTIA_BUILD_OK '+json.dumps(report),flush=True)


def render_views():
    bpy.ops.wm.open_mainfile(filepath=str(OUT/'molar_16.blend'))
    scene=bpy.context.scene
    for name,filename in [('Camara | Tres cuartos','molar_16_preview.png'),
                          ('Camara | Oclusal','molar_16_occlusal.png'),
                          ('Camara | Lateral','molar_16_lateral.png')]:
        scene.camera=bpy.data.objects[name]
        scene.render.filepath=str(OUT/filename)
        bpy.ops.render.render(write_still=True)
        print('DENTIA_RENDER_OK '+filename,flush=True)


if __name__=='__main__':
    args=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
    if '--render' in args: render_views()
    else: build()
