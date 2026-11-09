package com.dentia.dto.interno.ventas;

public record DocumentoExternoResultadoDto(
        boolean disponible,
        boolean encontrado,
        String nombres,
        String apellidos,
        String razonSocial,
        String direccion,
        String fechaNacimiento,
        String mensaje
) {
    public DocumentoExternoResultadoDto(boolean disponible, boolean encontrado, String nombres, String apellidos, String razonSocial, String direccion, String mensaje) {
        this(disponible, encontrado, nombres, apellidos, razonSocial, direccion, "", mensaje);
    }

    public static DocumentoExternoResultadoDto noEncontrado() {
        return new DocumentoExternoResultadoDto(true, false, "", "", "", "",
                "El documento no fue encontrado. Completa los datos manualmente.");
    }

    public static DocumentoExternoResultadoDto noDisponible(String mensaje) {
        return new DocumentoExternoResultadoDto(false, false, "", "", "", "", mensaje);
    }
}
