package com.dentia.dto.interno.clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteRankingDto(
        int posicion,
        Integer idCliente,
        String cliente,
        String documento,
        String clasificacion,
        long cantidadCompras,
        BigDecimal totalComprado,
        BigDecimal ticketPromedio,
        LocalDateTime ultimaCompra,
        String sedePrincipal,
        String canalPrincipal
) {
}
