package com.dentia.dto.interno.clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResumenComercialDto(
        Integer idCliente,
        String clasificacion,
        long cantidadCompras,
        BigDecimal totalHistorico,
        BigDecimal ticketPromedio,
        LocalDateTime primeraCompra,
        LocalDateTime ultimaCompra,
        String sedePrincipal,
        String canalPrincipal,
        String metodoPagoPrincipal,
        long cantidadCanales,
        long cantidadSedes
) {
}
