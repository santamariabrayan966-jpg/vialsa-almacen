package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Usuario;

import java.util.List;
import java.util.Optional;
public interface UsuarioDao {

    Optional<Usuario> findByNombreUsuario(String username);

    void ensureAdminUserExists(String username, String rawPassword);

    List<Usuario> listarTodos();

    Usuario obtenerPorId(int idUsuario);

    void crear(Usuario usuario);

    void actualizar(Usuario usuario);

    void actualizarRol(int idUsuario, int idRol);

    void crearUsuario(String nombreUsuario, String passwordPlano, int idRol);

    void eliminarUsuario(int idUsuario);

    Optional<Usuario> obtenerPorNombre(String nombreUsuario);

    void actualizarPerfil(Usuario usuario);

    void cambiarEstadoActivo(int idUsuario, boolean activo);

    void cambiarEstadoActivoPorRol(int idRol, boolean activo);

    Optional<Usuario> findByCorreo(String correo);

    void crearClienteDesdeGoogle(Usuario usuario);

    boolean existeCorreo(String correo);

    boolean existeDocumento(String documento);

    // NUEVO: solo usuarios internos (no CLIENTE)
    List<Usuario> listarUsuariosInternos();
}
