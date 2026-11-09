package com.dentia.dto.interno;

import java.util.List;

public record ProveedorComprasResponseDto(
        ProveedorComprasResumenDto resumen,
        List<ProveedorSedeResumenDto> comprasPorSede,
        List<ProveedorCompraDto> compras,
        List<String> estados,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {
}
