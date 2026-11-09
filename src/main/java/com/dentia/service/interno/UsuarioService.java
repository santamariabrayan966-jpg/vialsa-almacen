package com.dentia.service.interno;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.interno.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {
    private final UsuarioDao dao;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioDao dao, PasswordEncoder encoder){this.dao=dao;this.encoder=encoder;}
    public List<Usuario> listarUsuariosInternos(){return dao.listarUsuariosInternos();}
    public Usuario buscarPorId(Integer id){return id==null?null:dao.obtenerPorId(id);}

    public List<Usuario> buscarUsuarios(String texto,Integer rol,Integer estado){
        String q=texto==null?"":texto.trim().toLowerCase(Locale.ROOT);
        return listarUsuariosInternos().stream()
                .filter(u->rol==null||rol.equals(u.getIdRol()))
                .filter(u->estado==null||estado.equals(u.getIdEstadoUsuario()))
                .filter(u->q.isBlank()||(valor(u.getNombres())+" "+valor(u.getApellidos())+" "+valor(u.getNombreUsuario())+" "+valor(u.getNroDocumento())+" "+valor(u.getCorreo())).toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public void guardarUsuarioInterno(Usuario usuario){
        normalizarDatosDeAcceso(usuario);
        validar(usuario,true); validarContrasena(usuario.getContrasena());
        usuario.setContrasena(encoder.encode(usuario.getContrasena()));
        usuario.setIdTipoDocumento(usuario.getIdTipoDocumento()==null?1:usuario.getIdTipoDocumento());
        usuario.setIdEstadoUsuario(1); usuario.setIdSedePrincipal(1); usuario.setPuedeCambiarSede(false);
        dao.crear(usuario);
    }

    public void actualizarUsuarioInterno(Usuario usuario,String nuevaContrasena,String contrasenaActual){
        normalizarDatosDeAcceso(usuario);
        validar(usuario,false); Usuario original=buscarPorId(usuario.getIdUsuario());
        if(original==null) throw new IllegalArgumentException("El usuario no existe.");
        if(esAdmin(original)&&!original.getIdRol().equals(usuario.getIdRol())&&esUltimoAdminActivo(original.getIdUsuario()))
            throw new IllegalStateException("No se puede quitar el rol al unico ADMIN activo.");
        if(nuevaContrasena!=null&&!nuevaContrasena.isBlank()){validarContrasena(nuevaContrasena);usuario.setContrasena(encoder.encode(nuevaContrasena));}
        else usuario.setContrasena(contrasenaActual);
        usuario.setIdSedePrincipal(1);usuario.setPuedeCambiarSede(false);usuario.setIdEstadoUsuario(original.getIdEstadoUsuario());
        dao.actualizar(usuario);
    }

    public void restablecerContrasena(Integer id,String password){if(buscarPorId(id)==null)throw new IllegalArgumentException("El usuario no existe.");validarContrasena(password);dao.actualizarContrasena(id,encoder.encode(password));}
    public void eliminar(Integer id){if(id!=null){if(esUltimoAdminActivo(id))throw new IllegalStateException("No se puede eliminar el unico ADMIN activo.");dao.eliminarLogico(id);}}
    public void cambiarEstadoUsuario(Integer id,Integer estado){if(id==null||estado==null)return;if(estado!=1&&esUltimoAdminActivo(id))throw new IllegalStateException("No se puede desactivar el unico ADMIN activo.");dao.cambiarEstadoUsuario(id,estado);}
    public void cambiarEstadoUsuarioPorRol(Integer rol,Integer estado){dao.cambiarEstadoUsuarioPorRol(rol,estado);}
    public boolean esUltimoAdminActivo(Integer id){Usuario actual=buscarPorId(id);if(!esAdmin(actual)||!Integer.valueOf(1).equals(actual.getIdEstadoUsuario()))return false;return dao.listarTodos().stream().noneMatch(u->!id.equals(u.getIdUsuario())&&esAdmin(u)&&Integer.valueOf(1).equals(u.getIdEstadoUsuario()));}

    public Usuario obtenerUsuarioActual(){Authentication auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!auth.isAuthenticated())return null;String nombre=auth.getPrincipal() instanceof UserDetails u?u.getUsername():auth.getName();return dao.obtenerPorNombre(nombre).orElse(null);}
    public void actualizarPerfil(Usuario u){Usuario actual=buscarPorId(u.getIdUsuario());if(actual==null)throw new IllegalArgumentException("El usuario no existe.");u.setIdRol(actual.getIdRol());u.setIdEstadoUsuario(actual.getIdEstadoUsuario());u.setIdSedePrincipal(1);validar(u,false);u.setContrasena(null);dao.actualizarPerfil(u);}
    public void cambiarContrasenaPropia(Integer id,String actual,String nueva){Usuario usuario=buscarPorId(id);if(usuario==null||actual==null||!encoder.matches(actual,usuario.getContrasena()))throw new IllegalArgumentException("La contraseña actual no es correcta.");validarContrasena(nueva);dao.actualizarContrasena(id,encoder.encode(nueva));}
    public Usuario obtenerPorNombreUsuario(String nombre){return nombre==null?null:dao.obtenerPorNombre(nombre).orElse(null);}

    private void normalizarDatosDeAcceso(Usuario usuario) {
        if (usuario == null) return;
        usuario.setNombreUsuario(usuario.getNombreUsuario() == null ? null : usuario.getNombreUsuario().trim());
        usuario.setNroDocumento(opcional(usuario.getNroDocumento()));
        String correo = opcional(usuario.getCorreo());
        usuario.setCorreo(correo == null ? null : correo.toLowerCase(Locale.ROOT));
        // Un único correo registrado sirve para iniciar sesión y recuperar el acceso.
        usuario.setCorreoRecuperacion(usuario.getCorreo());
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validar(Usuario u,boolean nuevo){
        if(u==null||vacio(u.getNombreUsuario())||vacio(u.getNombres())||vacio(u.getApellidos())||u.getIdRol()==null)throw new IllegalArgumentException("Usuario, nombres, apellidos y rol son obligatorios.");
        if(u.getCorreo()!=null&&!u.getCorreo().isBlank()&&!u.getCorreo().matches("^[\\w.+'-]+@[\\w.-]+\\.[A-Za-z]{2,}$"))throw new IllegalArgumentException("Correo electronico invalido.");
        if(u.getNroDocumento()!=null&&!u.getNroDocumento().isBlank()&&!u.getNroDocumento().matches("\\d{8}"))throw new IllegalArgumentException("El DNI debe tener 8 digitos.");
        dao.obtenerPorNombre(u.getNombreUsuario().trim()).filter(x->nuevo||!x.getIdUsuario().equals(u.getIdUsuario())).ifPresent(x->{throw new IllegalArgumentException("El nombre de usuario ya existe.");});
        if(u.getCorreo()!=null&&!u.getCorreo().isBlank())dao.findByCorreo(u.getCorreo().trim()).filter(x->nuevo||!x.getIdUsuario().equals(u.getIdUsuario())).ifPresent(x->{throw new IllegalArgumentException("El correo ya esta registrado.");});
    }
    private void validarContrasena(String p){if(p==null||!p.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,80}$"))throw new IllegalArgumentException("La contrasena debe tener 8 caracteres, mayuscula, minuscula, numero y simbolo.");}
    private boolean esAdmin(Usuario u){return u!=null&&"ADMIN".equalsIgnoreCase(u.getNombreRol());}
    private boolean vacio(String v){return v==null||v.isBlank();} private String valor(String v){return v==null?"":v;}
}
