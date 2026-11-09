package com.dentia.dto.interno.clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ClienteComprasPaginaDto(
        List<ClienteCompraDto> compras,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        long comprasFiltradas,
        BigDecimal totalFiltrado,
        BigDecimal ticketPromedioFiltrado,
        LocalDateTime ultimaCompra,
        long canalesUtilizados,
        long sedesUtilizadas
) {
}
