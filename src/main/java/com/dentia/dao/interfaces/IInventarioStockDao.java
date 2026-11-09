package com.dentia.dao.interfaces;

import com.dentia.model.interno.inventario.InventarioStock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IInventarioStockDao {

    List<InventarioStock> listarExistencias();

    Optional<InventarioStock> buscarPorProductoYSede(Integer idProducto, Integer idSede);

    /**
     * Usa SELECT ... FOR UPDATE. Debe invocarse solo dentro de una transaccion activa.
     */
    Optional<InventarioStock> bloquearPorProductoYSede(Integer idProducto, Integer idSede);

    /**
     * Bloquea en orden fijo por idSede, idProducto. Debe invocarse dentro de una transaccion activa.
     */
    List<InventarioStock> bloquearPorSedeYProductos(Integer idSede, List<Integer> idsProducto);

    void crearStockInicial(Integer idProducto, Integer idSede, BigDecimal stockInicial, BigDecimal costoPromedio);

    void actualizarStock(Integer idProducto, Integer idSede, BigDecimal nuevoStock);

    int descontarStockCondicional(Integer idProducto, Integer idSede, BigDecimal cantidad);

    void actualizarStockYCostoPromedio(
            Integer idProducto,
            Integer idSede,
            BigDecimal nuevoStock,
            BigDecimal costoPromedio
    );

    int contarProductosConStock();

    int contarProductosConStockPorSede(Integer idSede);

    BigDecimal obtenerStockTotal();

    BigDecimal obtenerStockTotalPorSede(Integer idSede);

    BigDecimal obtenerValorTotalInventario();

    BigDecimal obtenerValorTotalInventarioPorSede(Integer idSede);

    List<InventarioStock> listarExistenciasPorSede(Integer idSede);

    void recalcularStockDesdePartes(Integer idProducto, Integer idSede);

    void recalcularValorDesdePartes(Integer idProducto, Integer idSede);

    BigDecimal obtenerStockEquivalenteDesdePartes(Integer idProducto, Integer idSede);

    BigDecimal obtenerValorDisponibleDesdePartes(Integer idProducto, Integer idSede);
}
