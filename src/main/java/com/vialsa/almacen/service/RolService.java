package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.Jdbc.JdbcRolDao;
import com.vialsa.almacen.model.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolService {

    @Autowired
    private JdbcRolDao rolDao;

    public List<Rol> listarRoles() {
        return rolDao.listarRoles();
    }
}
