package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Rol;
import java.util.List;

public interface RolDao {
    List<Rol> listarRoles();
    Rol obtenerPorId(int idRol);
    void crearRol(Rol rol);
    void actualizarRol(Rol rol);
    void eliminarRol(int idRol);
}
