package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Proveedor;

import java.util.List;
import java.util.Optional;

public interface IProveedorDao {

    /**
     * Lista solo los proveedores activos (activo = 1).
     */
    List<Proveedor> listar();

    /**
     * Busca un proveedor por su ID (activo o inactivo).
     */
    Optional<Proveedor> buscarPorId(Integer id);

    /**
     * Busca un proveedor (activo o inactivo) por su número de documento.
     */
    Optional<Proveedor> buscarPorNroDocumento(String nroDocumento);

    /**
     * Inserta un nuevo proveedor.
     */
    int crear(Proveedor proveedor);

    /**
     * Actualiza los datos de un proveedor existente.
     */
    int actualizar(Proveedor proveedor);

    /**
     * "Elimina" un proveedor (soft delete): pone activo = 0.
     */
    int eliminar(Integer id);
    /**
     * Cambia el estado 'activo' de un proveedor.
     */
    int cambiarEstado(Integer id, boolean activo);
    int actualizarEstado(Integer id, boolean activo);
    List<Proveedor> listarActivos();

}
