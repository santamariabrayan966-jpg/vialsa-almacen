package com.dentia.dto.interno.ventas;

public record ClienteDocumentoConsultaDto(
        boolean encontrado,
        String origen,
        Integer idCliente,
        String tipoDocumento,
        String documento,
        String nombre,
        String nombres,
        String apellidos,
        String razonSocial,
        String telefono,
        String correo,
        String direccion,
        boolean activo,
        String mensaje
) {
}
