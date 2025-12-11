package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Venta;
import java.math.BigDecimal;
import java.util.List;

public interface IVentaDao {

    // Listado y consulta
    List<Venta> listar();
    Venta buscarPorId(int idVenta);

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
}
