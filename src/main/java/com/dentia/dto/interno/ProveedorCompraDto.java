package com.dentia.dto.interno;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProveedorCompraDto(
        Integer idCompra,
        LocalDateTime fechaCompra,
        String nroOrdenCompra,
        String tipoComprobante,
        String serie,
        String numero,
        String nroComprobante,
        String nombreProveedor,
        Integer idSede,
        String nombreSede,
        String nombreUsuario,
        BigDecimal subtotal,
        BigDecimal montoIgv,
        BigDecimal totalCompra,
        String moneda,
        String formaPago,
        String tipoRecepcion,
        String estado,
        String nroGuiaRemision,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento
) {
}
