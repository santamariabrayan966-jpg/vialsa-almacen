package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IProveedorDao;
import com.vialsa.almacen.model.Proveedor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProveedorService {

    private final IProveedorDao proveedorDao;

    public ProveedorService(IProveedorDao proveedorDao) {
        this.proveedorDao = proveedorDao;
    }

    // 📋 Listar todos los proveedores
    public List<Proveedor> listar() {
        return proveedorDao.listar();
    }

    // 🔍 Buscar por ID
    public Proveedor buscarPorId(int id) {
        return proveedorDao.buscarPorId(id).orElse(null);
    }

    // 💾 Guardar o actualizar
    public boolean guardar(Proveedor proveedor) {
        if (proveedor.getIdProveedor() != null && proveedor.getIdProveedor() > 0) {
            return proveedorDao.actualizar(proveedor) > 0;
        } else {
            return proveedorDao.crear(proveedor) > 0;
        }
    }

    // ❌ Eliminar proveedor
    public boolean eliminar(int id) {
        return proveedorDao.eliminar(id) > 0;
    }
}
