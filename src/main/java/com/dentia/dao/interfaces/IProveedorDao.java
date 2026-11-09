package com.dentia.dao.interfaces;

import com.dentia.dto.interno.ProveedorCompraDto;
import com.dentia.dto.interno.ProveedorComprasResumenDto;
import com.dentia.dto.interno.ProveedorSedeResumenDto;
import com.dentia.model.interno.Proveedor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IProveedorDao {

    /**
     * Lista solo los proveedores activos (activo = 1).
     */
    List<Proveedor> listar();

    List<Proveedor> listarPaginado(String busqueda, int limite, int desplazamiento);

    long contar(String busqueda);

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

    ProveedorComprasResumenDto obtenerResumenCompras(Integer idProveedor);

    List<ProveedorSedeResumenDto> obtenerComprasPorSede(Integer idProveedor);

    List<ProveedorCompraDto> listarCompras(
            Integer idProveedor,
            String busqueda,
            Integer idSede,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int limite,
            int desplazamiento
    );

    long contarCompras(
            Integer idProveedor,
            String busqueda,
            Integer idSede,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    );

    List<String> listarEstadosCompras(Integer idProveedor);

}
