package com.dentia.dto.interno;

import com.dentia.model.interno.Proveedor;

import java.util.List;

public record ProveedorPaginaDto(
        List<Proveedor> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        String busqueda
) {
    public int primerElemento() {
        return totalElementos == 0 ? 0 : pagina * tamanio + 1;
    }

    public long ultimoElemento() {
        return Math.min((long) (pagina + 1) * tamanio, totalElementos);
    }
}
