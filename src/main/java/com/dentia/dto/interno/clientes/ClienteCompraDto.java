package com.dentia.dto.interno.clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteCompraDto(
        Integer idVenta,
        String codigo,
        String tipoComprobante,
        String numeroComprobante,
        String canal,
        LocalDateTime fecha,
        String sede,
        String vendedor,
        String metodoPago,
        BigDecimal total,
        String estadoVenta,
        String estadoEntrega,
        boolean comprobanteDisponible,
        String detalleUrl,
        String comprobanteUrl
) {
}
