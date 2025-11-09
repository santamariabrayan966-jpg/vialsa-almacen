package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.Jdbc.JdbcRolDao;
import com.vialsa.almacen.model.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolService {

    private final JdbcRolDao rolDao;

    @Autowired
    public RolService(JdbcRolDao rolDao) {
        this.rolDao = rolDao;
    }

    // 📋 Listar roles (activos + inactivos)
    public List<Rol> listarRoles() {
        return rolDao.listarRoles();
    }

    // 🔍 Obtener un rol por ID
    public Rol obtenerPorId(int idRol) {
        return rolDao.obtenerPorId(idRol);
    }

    // ➕ Crear un nuevo rol
    public void crearRol(Rol rol) {
        rolDao.crearRol(rol);
    }

    // ✏️ Actualizar rol existente
    public void actualizarRol(Rol rol) {
        rolDao.actualizarRol(rol);
    }

    // 🗑️ Eliminar (soft delete)
    public void eliminarRol(int idRol) {
        rolDao.eliminarRol(idRol);
    }

    // 🔁 Cambiar estado activo / inactivo
    public void cambiarEstadoActivo(int idRol, boolean activo) {
        rolDao.cambiarEstadoActivo(idRol, activo);
    }
}
