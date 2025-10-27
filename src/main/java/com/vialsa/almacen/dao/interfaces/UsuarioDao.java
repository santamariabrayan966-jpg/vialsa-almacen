package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> findByNombreUsuario(String username);
    void ensureAdminUserExists(String username, String rawPassword);

    // nuevos métodos administrativos
    List<Usuario> listarTodos();
    Usuario obtenerPorId(int idUsuario);
    void actualizarRol(int idUsuario, int idRol);
    void crearUsuario(String nombreUsuario, String passwordPlano, int idRol);
    void eliminarUsuario(int idUsuario);
}
