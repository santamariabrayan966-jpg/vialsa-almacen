package com.dentia.dto.interno.clientes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteDetalleComercialDto(
        Integer id,
        String nombre,
        String tipoDocumento,
        String documento,
        String telefono,
        String correo,
        String direccion,
        boolean activo,
        boolean vip,
        boolean moroso,
        LocalDateTime fechaRegistro,
        String foto,
        String clasificacion,
        long cantidadCompras,
        BigDecimal totalHistorico,
        BigDecimal ticketPromedio,
        LocalDateTime primeraCompra,
        LocalDateTime ultimaCompra,
        String sedePrincipal,
        String canalPrincipal,
        String metodoPagoPrincipal
) {
}
