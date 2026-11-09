package com.dentia.dto.interno;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProveedorComprasResumenDto(
        long totalCompras,
        BigDecimal totalHistorico,
        LocalDateTime ultimaCompra,
        List<String> sedes
) {
    public static ProveedorComprasResumenDto vacio() {
        return new ProveedorComprasResumenDto(0, BigDecimal.ZERO, null, List.of());
    }
}
