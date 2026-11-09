import bpy,bmesh,json
import numpy as np
from mathutils import Vector
from pathlib import Path
OUT=Path(__file__).resolve().parent
SOURCE=Path('C:/Users/Brayan/Documents/DENTISTA/odontograma_cerrado.blend')

def setup():
    bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
    source_names=['cerrada_arcada_inferior','cerrada_arcada_superior']
    objs=[bpy.data.objects[n] for n in source_names]
    pts=[]
    for obj in objs:
        xyz=np.array([tuple(obj.matrix_world@v.co) for v in obj.data.vertices])
        xyz=xyz[:,[1,0,2]]*np.array([1,-1,1])
        pts.append(xyz)
    combined=np.concatenate(pts)
    center=(combined.min(axis=0)+combined.max(axis=0))/2
    factor=64/(combined[:,0].max()-combined[:,0].min())
    arrays=[(xyz-center)*factor for xyz in pts]
    names=['upper','lower']
    # Dispose source only in this in-memory analysis process.
    bpy.ops.wm.read_factory_settings(use_empty=True)
    output=[]
    with bpy.data.libraries.load(str(SOURCE)) as (src,dst):
        dst.objects=list(source_names)
    for obj,xyz,name in zip(dst.objects,arrays,names):
        obj.name=name; bpy.context.collection.objects.link(obj)
        obj.location=(0,0,0); obj.rotation_mode='XYZ'; obj.rotation_euler=(0,0,0);obj.scale=(1,1,1)
        for v,co in zip(obj.data.vertices,xyz):v.co=co
        bm=bmesh.new();bm.from_mesh(obj.data)
        bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.005)
        bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free()
        for p in obj.data.polygons:p.use_smooth=True
        obj.data.calc_loop_triangles()
        coords=np.array([v.co[:] for v in obj.data.vertices])
        faces=np.array([t.vertices[:] for t in obj.data.loop_triangles])
        norms=np.array([v.normal[:] for v in obj.data.vertices])
        np.savez_compressed(OUT/(name+'.npz'),vertices=coords,faces=faces,normals=norms)
        output.append(obj)
    (OUT/'normalization.json').write_text(json.dumps({'source':str(SOURCE),'source_names':dict(zip(names,source_names)),'factor_to_mm':factor,'source_center_after_axis_change':center.tolist(),'target_width_mm':64,'scale_is_design_assumption':True},indent=2))
    return output

objects=setup()
scene=bpy.context.scene
scene.render.engine='BLENDER_WORKBENCH'
scene.render.resolution_x=1200;scene.render.resolution_y=1100;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG'
scene.display.shading.light='STUDIO';scene.display.shading.studiolight_rotate_z=0.4
scene.display.shading.color_type='SINGLE';scene.display.shading.single_color=(.72,.72,.68)
scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True
scene.display.shading.cavity_type='BOTH';scene.display.shading.curvature_ridge_factor=1.5;scene.display.shading.curvature_valley_factor=1.5
scene.world=bpy.data.worlds.new('AnalysisWorld')
scene.display.shading.background_type='WORLD';scene.world.color=(.1,.1,.1)
camdata=bpy.data.cameras.new('Analysis');camdata.type='ORTHO';camdata.ortho_scale=76;camdata.clip_end=1000
cam=bpy.data.objects.new('Analysis',camdata);scene.collection.objects.link(cam);scene.camera=cam
for name,pos,target in [('closed',(90,-145,60),(0,0,0)),('front',(0,-180,0),(0,0,0)),('upper',(0,0,-160),(0,0,0)),('lower',(0,0,160),(0,0,0))]:
    for obj in objects:obj.hide_render=name in ['upper','lower'] and obj.name!=name
    cam.location=pos;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(OUT/('source_'+name+'.png'));bpy.ops.render.render(write_still=True)
for obj in objects:obj.hide_render=False
scene.camera.location=(100,-160,55)
scene.camera.rotation_euler=(Vector((0,0,0))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
scene.camera.hide_set(True)
for screen in bpy.data.screens:
    for area in screen.areas:
        if area.type=='VIEW_3D':
            s=area.spaces.active;s.clip_end=10000;s.overlay.show_floor=False
            s.region_3d.view_location=(0,0,0);s.region_3d.view_distance=120
            s.region_3d.view_rotation=scene.camera.rotation_euler.to_quaternion()
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'source_normalized.blend'))
print('NORMALIZED_ARCHES_OK')
