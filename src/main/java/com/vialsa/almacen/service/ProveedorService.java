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

    // Alias opcional
    public Proveedor obtenerPorId(Integer id) {
        return buscarPorId(id);
    }

    // 💾 Guardar / actualizar / reactivar con lógica de negocio
    public ResultadoGuardarProveedor guardar(Proveedor proveedor) {

        try {
            // Normalizar campos de texto
            if (proveedor.getNroDocumento() != null) {
                proveedor.setNroDocumento(proveedor.getNroDocumento().trim());
            }
            if (proveedor.getNombreProveedor() != null) {
                proveedor.setNombreProveedor(proveedor.getNombreProveedor().trim());
            }
            if (proveedor.getDireccion() != null) {
                proveedor.setDireccion(proveedor.getDireccion().trim());
            }
            if (proveedor.getCorreo() != null) {
                proveedor.setCorreo(proveedor.getCorreo().trim());
            }
            if (proveedor.getTelefono() != null) {
                proveedor.setTelefono(proveedor.getTelefono().trim());
            }

            Integer id = proveedor.getIdProveedor();

            // 1️⃣ Si viene con id → ACTUALIZAR (manteniendo activo y fechaRegistro)
            if (id != null && id > 0) {
                Optional<Proveedor> optExistente = proveedorDao.buscarPorId(id);
                if (optExistente.isEmpty()) {
                    return ResultadoGuardarProveedor.ERROR;
                }

                Proveedor existente = optExistente.get();
                // Preservamos flags y fecha para no desactivar por error
                proveedor.setActivo(existente.isActivo());
                proveedor.setFechaRegistro(existente.getFechaRegistro());

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
                        // Estaba inactivo → REACTIVAR (usando los nuevos datos, pero activo = true)
                        proveedor.setIdProveedor(existente.getIdProveedor());
                        proveedor.setActivo(true);
                        proveedor.setFechaRegistro(existente.getFechaRegistro());

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
    public boolean cambiarEstado(Integer id, boolean activo) {
        if (id == null) return false;
        return proveedorDao.cambiarEstado(id, activo) > 0;
    }
    public boolean actualizarEstado(Proveedor proveedor) {
        try {
            return proveedorDao.actualizarEstado(proveedor.getIdProveedor(), proveedor.isActivo()) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<Proveedor> listarActivos() {
        return proveedorDao.listarActivos();
    }


}
