package com.dentia.dao.interfaces;

import com.dentia.model.interno.inventario.InventarioParte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IInventarioParteDao {

    int registrar(InventarioParte parte);

    List<InventarioParte> listarDisponiblesPorProductoYSede(
            Integer idProducto,
            Integer idSede
    );

    Optional<InventarioParte> buscarPorId(Integer idParte);

    /*
     * Busca y bloquea la parte física dentro de la transacción actual.
     * Debe implementarse con SELECT ... FOR UPDATE.
     */
    Optional<InventarioParte> buscarPorIdParaActualizar(Integer idParte);

    Optional<InventarioParte> buscarLoteCompletoDisponible(
            Integer idProducto,
            Integer idSede
    );

    Optional<InventarioParte> buscarLoteCompletoPorProductoYSede(
            Integer idProducto,
            Integer idSede
    );

    List<InventarioParte> listarGeneradasPorDetalleVenta(Integer idDetalleVenta);

    BigDecimal sumarEquivalenteDisponible(
            Integer idProducto,
            Integer idSede
    );

    BigDecimal sumarEquivalenteVendible(
            Integer idProducto,
            Integer idSede
    );

    BigDecimal sumarValorDisponible(
            Integer idProducto,
            Integer idSede
    );

    void actualizarDisponibilidadParte(
            Integer idParte,
            Integer medidaDisponible,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            String estado
    );

    /**
     * Restaura una pieza física individual consumida por una venta anulada.
     * A diferencia de actualizarDisponibilidadParte, también recupera
     * cantidadPartes = 1, porque al consumir un retazo/media individual
     * dicha cantidad queda en cero.
     */
    void restaurarParteIndividual(
            Integer idParte,
            Integer medidaDisponible,
            BigDecimal medidaFisicaDisponible,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            String estado
    );

    void actualizarLoteParte(
            Integer idParte,
            Integer cantidadPartes,
            Integer medidaDisponible,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            BigDecimal costoUnitarioParte,
            String estado
    );

    void convertirParteEnCorte(
            Integer idParte,
            String tipoParte,
            Integer medidaDisponible,
            Integer medidaPorParte,
            BigDecimal largoDisponibleMm,
            BigDecimal anchoDisponibleMm,
            Integer idParteOrigen,
            Integer idDetalleVentaOrigen,
            Integer numeroPlanoOrigen,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            BigDecimal costoUnitarioParte,
            BigDecimal costoPorMedida,
            String descripcion
    );

    void incrementarLoteParte(
            Integer idParte,
            Integer cantidadAgregar,
            Integer medidaAgregar,
            BigDecimal equivalenteAgregar,
            BigDecimal costoAgregar,
            String estado
    );

    void actualizarGeometriaParte(
            Integer idParte,
            BigDecimal largoDisponibleMm,
            BigDecimal anchoDisponibleMm,
            Integer idParteOrigen,
            Integer idDetalleVentaOrigen,
            Integer numeroPlanoOrigen
    );
}