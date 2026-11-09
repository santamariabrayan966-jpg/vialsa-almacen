package com.dentia.dao.interfaces;

import com.dentia.model.interno.Rol;
import java.util.List;

public interface RolDao {
    List<Rol> listarRoles();
    List<Rol> listarRolesInternos();
    Rol obtenerPorId(int idRol);
    int crearRol(Rol rol);
    void actualizarRol(Rol rol);
    void eliminarRol(int idRol);
    void cambiarEstadoActivo(int idRol, boolean activo);
    int contarUsuariosAsociados(int idRol);
}
