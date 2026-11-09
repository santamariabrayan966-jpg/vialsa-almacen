package com.dentia.dto.interno;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProveedorDetalleDto(
        Integer idProveedor,
        String nombreProveedor,
        String nroDocumento,
        String direccion,
        String telefono,
        String correo,
        Integer estado,
        LocalDateTime fechaRegistro,
        long totalCompras,
        BigDecimal totalHistorico,
        LocalDateTime ultimaCompra,
        List<String> sedes,
        boolean puedeVerCompras
) {
}
