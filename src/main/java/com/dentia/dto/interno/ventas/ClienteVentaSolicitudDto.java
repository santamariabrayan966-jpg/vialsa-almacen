package com.dentia.dto.interno.ventas;

public record ClienteVentaSolicitudDto(
        Integer idCliente,
        String documento,
        String nombres,
        String apellidos,
        String razonSocial,
        String nombreMostrado,
        String telefono,
        String correo,
        String direccion,
        String tipoComprobante,
        String origen
) {
}
