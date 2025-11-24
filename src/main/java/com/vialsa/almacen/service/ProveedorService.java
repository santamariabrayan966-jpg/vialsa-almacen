package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IProveedorDao;
import com.vialsa.almacen.model.Proveedor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    public enum ResultadoGuardarProveedor {
        NUEVO,
        ACTUALIZADO,
        REACTIVADO,
        DUPLICADO,
        ERROR
    }

    private final IProveedorDao proveedorDao;

    public ProveedorService(IProveedorDao proveedorDao) {
        this.proveedorDao = proveedorDao;
    }

    // 📋 Listar solo proveedores activos (el DAO ya filtra activo = 1)
    public List<Proveedor> listar() {
        return proveedorDao.listar();
    }

    // 🔍 Buscar por ID
    public Proveedor buscarPorId(Integer id) {
        if (id == null) return null;
        return proveedorDao.buscarPorId(id).orElse(null);
    }

    // Alias que ya usas en el Controller
    public Proveedor obtenerPorId(Integer id) {
        return buscarPorId(id);
    }

    // 💾 Guardar / actualizar / reactivar con lógica de negocio
    public ResultadoGuardarProveedor guardar(Proveedor proveedor) {

        try {
            // Normalizar número de documento
            if (proveedor.getNroDocumento() != null) {
                proveedor.setNroDocumento(proveedor.getNroDocumento().trim());
            }

            // 1️⃣ Si viene con id → ACTUALIZAR
            if (proveedor.getIdProveedor() != null && proveedor.getIdProveedor() > 0) {
                int filas = proveedorDao.actualizar(proveedor);
                return (filas > 0) ? ResultadoGuardarProveedor.ACTUALIZADO : ResultadoGuardarProveedor.ERROR;
            }

            // 2️⃣ Alta nueva: revisar por nroDocumento si ya existe
            String nroDoc = proveedor.getNroDocumento();
            if (nroDoc != null && !nroDoc.isBlank()) {
                Optional<Proveedor> optExistente = proveedorDao.buscarPorNroDocumento(nroDoc);

                if (optExistente.isPresent()) {
                    Proveedor existente = optExistente.get();

                    if (existente.isActivo()) {
                        // Ya hay uno ACTIVO con ese RUC/DNI → DUPLICADO
                        return ResultadoGuardarProveedor.DUPLICADO;
                    } else {
                        // Estaba inactivo → REACTIVAR
                        proveedor.setIdProveedor(existente.getIdProveedor());
                        proveedor.setActivo(true); // lo levantamos

                        int filas = proveedorDao.actualizar(proveedor);
                        return (filas > 0) ? ResultadoGuardarProveedor.REACTIVADO : ResultadoGuardarProveedor.ERROR;
                    }
                }
            }

            // 3️⃣ No existe en BD → crear NUEVO
            proveedor.setActivo(true);  // nuevo siempre activo
            int filas = proveedorDao.crear(proveedor);
            return (filas > 0) ? ResultadoGuardarProveedor.NUEVO : ResultadoGuardarProveedor.ERROR;

        } catch (Exception e) {
            e.printStackTrace();
            return ResultadoGuardarProveedor.ERROR;
        }
    }

    // ❌ "Eliminar": soft delete (activo = 0)
    public boolean eliminar(Integer id) {
        if (id == null) return false;
        return proveedorDao.eliminar(id) > 0;
    }
}
