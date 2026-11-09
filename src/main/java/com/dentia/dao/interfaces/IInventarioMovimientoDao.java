package com.dentia.dao.interfaces;

import com.dentia.model.interno.inventario.InventarioMovimiento;

import java.util.List;
import java.util.Optional;

public interface IInventarioMovimientoDao {

    List<InventarioMovimiento> listarMovimientos();

    List<InventarioMovimiento> listarMovimientosPorSede(Integer idSede);

    int registrarMovimiento(InventarioMovimiento movimiento);

    int contarMovimientos();

    int contarMovimientosPorSede(Integer idSede);

    List<InventarioMovimiento> listarMovimientosPorSedeConFiltros(
            Integer idSede,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            java.time.LocalDate fechaInicio,
            java.time.LocalDate fechaFin
    );

    int contarMovimientosPorSedeConFiltros(
            Integer idSede,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            java.time.LocalDate fechaInicio,
            java.time.LocalDate fechaFin
    );

    List<InventarioMovimiento> listarUltimosMovimientosPorProductoYSede(Integer idProducto, Integer idSede, int limite);
    Optional<InventarioMovimiento> buscarPorId(Integer idMovimiento);
}
