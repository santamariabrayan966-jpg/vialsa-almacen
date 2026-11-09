"""Build a reviewable, articulated odontogram from the supplied mesh.

Preserves the user's source and earlier attempts. Blender coordinates in mm
during construction; exported geometry uses metres. Roots are schematic.
"""
import bpy,bmesh,math,json,sys
import numpy as np
from pathlib import Path
from mathutils import Vector

OUT=Path(__file__).resolve().parent
MM=.001

def material(name,color,roughness):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    p=m.node_tree.nodes.get('Principled BSDF');p.inputs['Base Color'].default_value=(*color,1)
    p.inputs['Roughness'].default_value=roughness
    return m

def collection(name):
    c=bpy.data.collections.new(name);bpy.context.scene.collection.children.link(c);return c

def empty(name,col,parent=None,position=(0,0,0)):
    o=bpy.data.objects.new(name,None);col.objects.link(o);o.parent=parent
    o.location=position;o.empty_display_size=.002
    return o

def mesh_obj(name,verts,faces,col,mat,parent=None,position=(0,0,0),cap=False):
    mesh=bpy.data.meshes.new(name+'_mesh');mesh.from_pydata(verts,[],faces);mesh.update()
    bm=bmesh.new();bm.from_mesh(mesh)
    if cap:
        boundary=[e for e in bm.edges if e.is_boundary]
        if boundary:bmesh.ops.holes_fill(bm,edges=boundary,sides=0)
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    bmesh.ops.triangulate(bm,faces=list(bm.faces))
    bm.to_mesh(mesh);bm.free();mesh.update()
    for p in mesh.polygons:p.use_smooth=True
    o=bpy.data.objects.new(name,mesh);col.objects.link(o);o.parent=parent;o.location=position
    mesh.materials.append(mat)
    return o

def face_labels(v,f,labels,count):
    tri=labels[f]
    chosen=np.where(tri[:,0]==tri[:,1],tri[:,0],np.where(tri[:,1]==tri[:,2],tri[:,1],np.where(tri[:,0]==tri[:,2],tri[:,0],-1)))
    # Preserve one connected crown surface per object. Small islands are gum.
    for label in range(count):
        indices=np.flatnonzero(chosen==label)
        lookup={}
        for i in indices:
            for vertex in f[i]:lookup.setdefault(int(vertex),[]).append(int(i))
        todo=set(map(int,indices));groups=[]
        while todo:
            first=todo.pop();stack=[first];part=[first]
            while stack:
                current=stack.pop()
                for vertex in f[current]:
                    for neighbor in lookup[int(vertex)]:
                        if neighbor in todo:todo.remove(neighbor);stack.append(neighbor);part.append(neighbor)
            groups.append(part)
        if len(groups)>1:
            keep=max(groups,key=len)
            chosen[indices]=-1;chosen[keep]=label
    return chosen

def subset(v,f,mask):
    selected=f[mask];indices,inverse=np.unique(selected,return_inverse=True)
    return v[indices],inverse.reshape(-1,3)

def roots_geometry(width,depth,code,arch,neck):
    number=code%10 if isinstance(code,int) else 1
    count=3 if number>=6 and arch=='upper' else 2 if number>=6 or (arch=='upper' and number==4) else 1
    sign=1 if arch=='upper' else -1
    length=12 if number>=6 else 11 if number>=4 else 13 if number==3 else 10
    offsets=[(0,0)] if count==1 else [(-.19*width,0),(.19*width,0)] if count==2 else [(-.2*width,-.16*depth),(.2*width,-.16*depth),(0,.22*depth)]
    verts=[];faces=[];rings=22;sides=28
    for k,(ox,oy) in enumerate(offsets):
        start=len(verts);rad=min(width,depth)*(.32 if count==1 else .20)
        rad=max(.85,min(rad,2.2))
        for j in range(rings):
            t=j/rings;r=rad*(1-t)**.68
            cx=ox*(1+.7*t)+.35*math.sin(math.pi*t)
            cy=oy*(1+.65*t)
            for i in range(sides):
                angle=math.tau*i/sides
                verts.append((cx+r*math.cos(angle),cy+r*.83*math.sin(angle),neck+sign*(length*t-.8)))
        for j in range(rings-1):
            for i in range(sides):
                a=start+j*sides+i;b=start+j*sides+(i+1)%sides
                faces.append((a,b,b+sides,a+sides))
        top=len(verts);verts.append((ox,oy,neck-sign*.8))
        tip=len(verts);verts.append((ox*1.7,oy*1.65,neck+sign*(length-.8)))
        for i in range(sides):
            faces.append((start+i,top,start+(i+1)%sides))
            last=start+(rings-1)*sides
            faces.append((last+i,last+(i+1)%sides,tip))
    return verts,faces,count

def set_camera(scene,col,name,pos,target,scale):
    d=bpy.data.cameras.new(name);d.type='ORTHO';d.ortho_scale=scale;d.clip_start=.001;d.clip_end=5
    o=bpy.data.objects.new(name,d);col.objects.link(o);o.location=pos
    o.rotation_euler=(Vector(target)-o.location).to_track_quat('-Z','Y').to_euler();o.hide_set(True)
    return o

def build():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    s=bpy.context.scene;s.name='DentIA | Modelo articulado'
    s.unit_settings.system='METRIC';s.unit_settings.length_unit='MILLIMETERS'
    rigcol=collection('00 CONTROLES - abrir boca')
    master=empty('DENTIA_ODONTOGRAMA',rigcol)
    master['source']='User-provided odontograma_cerrado.blend'
    master['approximate']=True;master['tooth_count']=33
    master['additional_tooth']='EXTRA_L_01: crown present in source; no FDI assigned'
    upper=empty('MAXILAR_SUPERIOR',rigcol,master)
    pivot=Vector((0,.029,.002))
    jaw=empty('MANDIBULA - apertura 0 a 1',rigcol,master,pivot)
    jaw['apertura']=0.0;jaw.id_properties_ui('apertura').update(min=0,max=1,description='0 cerrada; 1 apertura ilustrativa de 32 grados')
    jaw['maximum_open_degrees']=32.;jaw['hinge_is_illustrative']=True
    driver=jaw.driver_add('rotation_euler',0).driver;driver.expression='apertura*0.5585053606'
    var=driver.variables.new();var.name='apertura';var.type='SINGLE_PROP';var.targets[0].id=jaw;var.targets[0].data_path='["apertura"]'
    for frame,value in [(1,0),(41,1),(81,0)]:
        jaw['apertura']=value;jaw.keyframe_insert(data_path='["apertura"]',frame=frame)
    s.frame_start=1;s.frame_end=81;s.render.fps=24
    enamel=material('Esmalte - marfil',(0.87,.83,.73),.30)
    gum=material('Encia - rosa',(0.48,.16,.19),.48)
    rootmat=material('Raices esquematicas',(0.66,.53,.35),.42)
    report=[];all_meshes=[]
    for arch,parent in [('upper',upper),('lower',jaw)]:
        data=np.load(OUT/f'{arch}_segmented.npz');v,f,labels=data['vertices'],data['faces'],data['labels']
        meta=json.loads((OUT/f'{arch}_segmentation.json').read_text())
        toothcol=collection(('01' if arch=='upper' else '02')+' DIENTES '+arch.upper())
        rootcol=collection(('03' if arch=='upper' else '04')+' RAICES '+arch.upper())
        gumcol=collection(('05' if arch=='upper' else '06')+' ENCIA '+arch.upper())
        assigned=face_labels(v,f,labels,len(meta['ids']))
        parent_offset=np.array(parent.location)/MM
        for i,code in enumerate(meta['ids']):
            cv,cf=subset(v,f,assigned==i)
            if len(cf)<50:raise RuntimeError('Crown missing '+str(code))
            # Cervical pivot from the tooth's extracted surface, not global origin.
            center=(cv.min(axis=0)+cv.max(axis=0))/2
            name=f'FDI_{code}' if isinstance(code,int) else str(code)
            tooth=empty(name,toothcol,parent,(center-parent_offset)*MM)
            tooth['fdi']=code if isinstance(code,int) else ''
            tooth['tooth_id']=name;tooth['arch']=arch;tooth['dentition']='permanent'
            tooth['additional']=not isinstance(code,int)
            crown=mesh_obj(name+'_CORONA',(cv-center).tolist(),cf.tolist(),toothcol,enamel,tooth,cap=True)
            crown['part']='crown';crown['tooth_id']=name
            for vert in crown.data.vertices:vert.co*=MM
            bounds=cv.max(axis=0)-cv.min(axis=0)
            neck=(np.percentile(cv[:,2],90) if arch=='upper' else np.percentile(cv[:,2],10))-center[2]
            rv,rf,rc=roots_geometry(bounds[0],bounds[1],code,arch,neck)
            root=mesh_obj(name+'_RAICES',rv,rf,rootcol,rootmat,tooth)
            for vert in root.data.vertices:vert.co*=MM
            root['part']='roots';root['tooth_id']=name;root['schematic']=True;root['root_count']=rc
            report.append({'tooth_id':name,'fdi':code if isinstance(code,int) else None,'arch':arch,'root_count':rc,'source_crown_faces':len(cf),'center_mm':center.tolist()})
            all_meshes.extend([crown,root])
        gv,gf=subset(v,f,assigned==-1)
        gums=mesh_obj('ENCIA_'+arch.upper(),(gv-parent_offset).tolist(),gf.tolist(),gumcol,gum,parent,cap=True)
        for vert in gums.data.vertices:vert.co*=MM
        gums['part']='gingiva';gums['arch']=arch;all_meshes.append(gums)
    # Reduce the original model's long stand-like gingival bases for presentation.
    # Geometry below the cervical region is only a display support, not bone.
    for o in all_meshes:
        if o.get('part')=='gingiva':
            for v in o.data.vertices:
                world_z=v.co.z+(pivot.z if o['arch']=='lower' else 0)
                if abs(world_z)>.013:
                    new_z=math.copysign(.013+(abs(world_z)-.013)*.52,world_z)
                    v.co.z=new_z-(pivot.z if o['arch']=='lower' else 0)
    studio=collection('07 PRESENTACION')
    s.world=bpy.data.worlds.new('Fondo');s.world.color=(.075,.085,.105)
    s.render.engine='BLENDER_WORKBENCH'
    sh=s.display.shading;sh.light='STUDIO';sh.color_type='MATERIAL';sh.show_shadows=True
    sh.show_cavity=True;sh.cavity_type='BOTH';sh.curvature_ridge_factor=1.0;sh.curvature_valley_factor=1.1
    sh.background_type='WORLD';sh.show_specular_highlight=True
    s.render.resolution_x=1500;s.render.resolution_y=1200;s.render.resolution_percentage=100
    s.render.image_settings.file_format='PNG'
    cam=set_camera(s,studio,'Camara tres cuartos',(.10,-.16,.075),(0,0,-.007),.103)
    s.camera=cam
    set_camera(s,studio,'Camara frontal',(0,-.19,.01),(0,0,-.008),.09)
    set_camera(s,studio,'Camara superior',(0,0,-.17),(0,0,0),.08)
    set_camera(s,studio,'Camara inferior',(0,0,.17),(0,0,0),.08)
    notes=bpy.data.texts.new('LEEME - Controles')
    notes.write('DentIA - modelo articulado\n\nESPACIO reproduce abrir/cerrar. Fotograma 1: cerrada; 41: abierta.\nSelecciona MANDIBULA - apertura 0 a 1; en Propiedades personalizadas cambia apertura (0 a 1).\nCada FDI es un grupo independiente con CORONA y RAICES.\nLas colecciones 03/04 permiten ocultar todas las raices. Las 05/06 permiten ocultar encias.\nLas coronas proceden del modelo aportado. Las raices son aproximaciones propias.\nSe conserva una corona adicional inferior como EXTRA_L_01, sin numero FDI.\nEl eje de apertura es ilustrativo, no una simulacion individual de ATM.\n')
    for screen in bpy.data.screens:
        for area in screen.areas:
            if area.type=='VIEW_3D':
                space=area.spaces.active;space.clip_start=.0001;space.clip_end=10
                space.overlay.show_floor=False;space.overlay.show_axis_x=False;space.overlay.show_axis_y=False
                space.shading.color_type='MATERIAL';space.shading.show_cavity=True
                space.region_3d.view_rotation=cam.rotation_euler.to_quaternion()
                space.region_3d.view_location=(0,0,-.009);space.region_3d.view_distance=.145
    # Web geometry is reduced using temporary modifiers; detailed .blend retained.
    bpy.ops.object.select_all(action='DESELECT')
    for o in bpy.context.scene.objects:
        if o.type in ['EMPTY','MESH']:o.select_set(True)
    modifiers=[]
    for o in all_meshes:
        dec=o.modifiers.new('Detalle web','DECIMATE')
        dec.ratio=.40 if o.get('part')=='crown' else .23 if o.get('part')=='gingiva' else .6
        modifiers.append((o,dec))
    s.frame_set(1)
    bpy.ops.export_scene.gltf(filepath=str(OUT/'dentia_articulado.glb'),export_format='GLB',use_selection=True,export_apply=True,
         export_extras=True,export_animations=True,export_animation_mode='SCENE',export_force_sampling=True,export_frame_range=True,export_cameras=False,export_lights=False)
    for o,mod in modifiers:o.modifiers.remove(mod)
    s.frame_set(29)
    bpy.ops.object.select_all(action='DESELECT');jaw.select_set(True);bpy.context.view_layer.objects.active=jaw
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'dentia_articulado.blend'))
    (OUT/'model_manifest.json').write_text(json.dumps({'teeth':report,'crown_count':len(report),'web_bytes':(OUT/'dentia_articulado.glb').stat().st_size,
        'units':'metres','frames':{'closed':1,'open':41,'closed_again':81},'roots':'schematic','version':'2.0'},indent=2))
    print('DENTIA_BUILD_OK',len(report),(OUT/'dentia_articulado.glb').stat().st_size,flush=True)
    render(s)

def render(s):
    for frame,name in [(1,'cerrada'),(41,'abierta')]:
        s.frame_set(frame);s.render.filepath=str(OUT/f'dentia_{name}.png');bpy.ops.render.render(write_still=True)
    # Extra inspection with the gum hidden: every crown and root visible.
    for o in s.objects:
        if o.get('part')=='gingiva':o.hide_render=True
    s.frame_set(41);s.render.filepath=str(OUT/'dentia_dientes_raices.png');bpy.ops.render.render(write_still=True)
    print('DENTIA_PREVIEWS_OK',flush=True)

if __name__=='__main__':build()
