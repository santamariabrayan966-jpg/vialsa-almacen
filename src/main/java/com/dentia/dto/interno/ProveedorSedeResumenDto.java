package com.dentia.dto.interno;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProveedorSedeResumenDto(
        Integer idSede,
        String nombreSede,
        long cantidadCompras,
        BigDecimal totalComprado,
        LocalDateTime ultimaCompra
) {
}
