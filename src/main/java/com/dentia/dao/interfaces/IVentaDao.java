package com.dentia.dao.interfaces;

import com.dentia.model.interno.Venta;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

public interface IVentaDao {

    // Listado y consulta
    List<Venta> listar();
    Venta buscarPorId(int idVenta);
    Venta buscarPedidoEntregaPorId(Integer idPedido);

    // Registrar venta
    int registrarBorrador(Venta venta);
    int registrarYObtenerId(Venta venta);

    // Actualizar
    int actualizarVenta(Venta venta);
    int actualizarEstado(int idVenta, String nuevoEstado);
    int actualizarDeuda(int idVenta, BigDecimal nuevaDeuda);

    // Pagos
    int registrarPagoInicial(int idVenta, BigDecimal pagoInicial);

    // Comprobante
    String obtenerSiguienteComprobante(String tipoComprobante);

    // Usuario
    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);

    // Eliminación y anulación
    int eliminarVenta(int idVenta);      // Solo borrador
    int anularVenta(int idVenta);
    // Completa → Anulada

    int registrarVentaOnline(Venta venta);

    int registrarVentaOnlineAprobada(Venta venta);

    Venta buscarPorIdOrdenOnline(Integer idOrdenOnline);

    Venta buscarPorCodigoTicket(String codigoTicket);

    Integer obtenerUltimoId();

    List<Venta> filtrar(
            String canal,
            String estado,
            String estadoEntrega,
            String buscar,
            String fechaDesde,
            String fechaHasta
    );

    List<Venta> filtrarPorSede(
            Integer idSede,
            String canal,
            String estado,
            String estadoEntrega,
            String buscar,
            String fechaDesde,
            String fechaHasta
    );

    void registrarEntregaInmediata(int idVenta);

    void registrarEntregaProgramada(int idVenta, LocalDateTime fechaRecojoProgramada);

    void marcarCreditoPagado(Integer idVenta);

    void disminuirDeudaCredito(Integer idVenta, BigDecimal montoPago);

    // ======================================
// ENTREGAS
// ======================================

    List<Venta> listarPendientesEntrega();

    List<Venta> listarPendientesEntregaPorSede(Integer idSede);

    Venta obtenerPedidoActualPorUsuario(Integer idUsuario);

    Venta obtenerPedidoActualPorUsuarioYSede(Integer idUsuario, Integer idSede);

    List<Venta> listarPedidosListos();

    List<Venta> listarPedidosListosPorSede(Integer idSede);

    Venta buscarPorCodigoRecojo(String codigo);

    Venta buscarPorCodigoRecojoYSede(String codigo, Integer idSede);

    Venta buscarPedidoListoParaEntrega(String texto);

    Venta buscarPedidoListoParaEntregaPorSede(String texto, Integer idSede);

    boolean existeCodigoRecojo(String codigo);

    void aceptarPedido(Integer idPedido,
                       Integer idUsuario);

    void marcarPedidoListo(Integer idPedido);

    void confirmarEntrega(Integer idPedido,
                          Integer idUsuarioEntrega);

    List<Venta> listarHistorialEntregas();

    List<Venta> listarHistorialEntregasPorSede(Integer idSede);

    long contarEntregadosHoy();

    long contarEntregadosHoyPorSede(Integer idSede);

    void anularPedidoPorVenta(Integer idVenta);

    long contarPreparando();

    long contarPreparandoPorSede(Integer idSede);

    long contarEntregasInmediatasHoy();

    long contarEntregasInmediatasHoyPorSede(Integer idSede);

    long contarEntregasDespachoHoy();

    long contarEntregasDespachoHoyPorSede(Integer idSede);

    long contarAnuladosHoy();

    long contarAnuladosHoyPorSede(Integer idSede);

    // ======================================
// DASHBOARD / ANÁLISIS
// ======================================

    Map<String, Object> obtenerResumenDashboard(Integer idSede, String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarVentasPorDiaDashboard(Integer idSede, String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarVentasPorCategoriaDashboard(Integer idSede, String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarVentasPorSedeDashboard(String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarVentasPorCanalDashboard(Integer idSede, String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarVentasPorFormaPagoDashboard(Integer idSede, String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarUltimasVentasDashboard(Integer idSede, String fechaDesde, String fechaHasta);

    List<Map<String, Object>> listarTopProductosDashboard(Integer idSede, String fechaDesde, String fechaHasta);
}
