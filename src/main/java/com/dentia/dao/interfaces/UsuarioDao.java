package com.dentia.dao.interfaces;

import com.dentia.model.interno.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioDao {

    Optional<Usuario> findByNombreUsuario(String username);

    boolean ensureAdminUserExists(String username, String passwordHash);

    List<Usuario> listarTodos();

    Usuario obtenerPorId(int idUsuario);

    void crear(Usuario usuario);

    void actualizar(Usuario usuario);

    void actualizarRol(int idUsuario, int idRol);
    void actualizarContrasena(int idUsuario, String passwordHash);

    void crearUsuario(String nombreUsuario, String passwordPlano, int idRol);

    void eliminarLogico(int idUsuario);

    Optional<Usuario> obtenerPorNombre(String nombreUsuario);

    void actualizarPerfil(Usuario usuario);

    void cambiarEstadoUsuario(int idUsuario, int idEstadoUsuario);

    void cambiarEstadoUsuarioPorRol(int idRol, int idEstadoUsuario);

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByCorreoRecuperacion(String correo);

    void crearClienteDesdeGoogle(Usuario usuario);

    boolean existeCorreo(String correo);

    boolean existeDocumento(String documento);

    List<Usuario> listarUsuariosInternos();

    // ======================
    // SEGURIDAD LOGIN
    // ======================

    void incrementarIntentosFallidos(String username);

    void resetearIntentosFallidos(String username);

    int obtenerIntentosFallidos(String username);

    void bloquearTemporalmente(String username);

    boolean estaBloqueado(String username);
}
