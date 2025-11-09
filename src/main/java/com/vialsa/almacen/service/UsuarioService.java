package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioDao usuarioDao;

    // ✅ Inyección por constructor
    public UsuarioService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    // 📋 Listar todos los usuarios
    public List<Usuario> listarUsuarios() {
        return usuarioDao.listarTodos();
    }

    // 🔍 Buscar usuario por ID
    public Usuario buscarPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return usuarioDao.obtenerPorId(id);
    }

    // 💾 Crear nuevo usuario (desde el formulario "Nuevo usuario")
    public void guardar(Usuario usuario) {
        // Ahora usamos el método crear(Usuario) que guarda TODOS los campos
        usuarioDao.crear(usuario);
    }

    // 💾 Actualizar TODOS los datos del usuario (usado por el modal de edición)
    public void actualizar(Usuario usuario) {
        if (usuario.getIdUsuario() != null) {
            usuarioDao.actualizar(usuario);
        }
    }

    // 🗑️ Eliminar usuario
    public void eliminar(Integer id) {
        if (id != null) {
            usuarioDao.eliminarUsuario(id);
        }
    }

    // 👤 Obtener el usuario actualmente autenticado
    public Usuario obtenerUsuarioActual() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        return usuarioDao.obtenerPorNombre(username).orElse(null);
    }

    // 💾 Actualizar datos del perfil (incluye posible cambio de contraseña)
    public void actualizarPerfil(Usuario usuario) {
        usuarioDao.actualizarPerfil(usuario);
    }

    // ✅ Activar / desactivar usuario (para el botón en la tabla)
    public void cambiarEstadoActivo(Integer idUsuario, boolean activo) {
        if (idUsuario != null) {
            usuarioDao.cambiarEstadoActivo(idUsuario, activo);
        }
    }

    // ✅ NUEVO: Activar / desactivar TODOS los usuarios de un rol
    public void cambiarEstadoActivoPorRol(Integer idRol, boolean activo) {
        if (idRol != null) {
            usuarioDao.cambiarEstadoActivoPorRol(idRol, activo);
        }
    }
}
