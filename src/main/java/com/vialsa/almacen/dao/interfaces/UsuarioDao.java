package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioDao {

    // Usado por Spring Security
    Optional<Usuario> findByNombreUsuario(String username);
    void ensureAdminUserExists(String username, String rawPassword);

    // Listar / obtener
    List<Usuario> listarTodos();
    Usuario obtenerPorId(int idUsuario);

    // 🔹 Crear un usuario completo desde el formulario (nuevo usuario)
    void crear(Usuario usuario);

    // 🔹 Actualizar TODOS los datos del usuario (para el modal /editar)
    void actualizar(Usuario usuario);

    // Otros métodos específicos
    void actualizarRol(int idUsuario, int idRol);

    // Usado tal vez solo para bootstrap de admin (username + pass + rol)
    void crearUsuario(String nombreUsuario, String passwordPlano, int idRol);

    void eliminarUsuario(int idUsuario);

    Optional<Usuario> obtenerPorNombre(String nombreUsuario);

    // Actualización de perfil (puedes diferenciar lógica si quieres)
    void actualizarPerfil(Usuario usuario);

    // Activar / desactivar 1 usuario
    void cambiarEstadoActivo(int idUsuario, boolean activo);

    // ✅ NUEVO: activar / desactivar TODOS los usuarios de un rol
    void cambiarEstadoActivoPorRol(int idRol, boolean activo);
}
