package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.Jdbc.JdbcPermisoDao;
import com.vialsa.almacen.model.Permiso;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PermisoService {

    private final JdbcPermisoDao permisoDao;

    public PermisoService(JdbcPermisoDao permisoDao) {
        this.permisoDao = permisoDao;
    }

    public List<Permiso> obtenerPorRol(int idRol) {
        return permisoDao.obtenerPorRol(idRol);
    }

    public void guardarPermisos(int idRol, List<Permiso> permisos) {
        permisoDao.guardarPermisos(idRol, permisos);
    }
}
