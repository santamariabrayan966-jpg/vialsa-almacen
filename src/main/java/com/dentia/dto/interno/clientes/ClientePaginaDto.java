package com.dentia.dto.interno.clientes;

import com.dentia.model.interno.Cliente;

import java.util.List;
import java.util.Map;

public record ClientePaginaDto(
        List<Cliente> clientes,
        Map<Integer, ClienteResumenComercialDto> resumenes,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        long desde,
        long hasta
) {
}
