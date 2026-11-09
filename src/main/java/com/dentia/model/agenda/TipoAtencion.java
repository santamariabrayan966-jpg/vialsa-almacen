package com.dentia.model.agenda;

public record TipoAtencion(
        Long idTipoAtencion,
        String nombre,
        String descripcion,
        int duracionMinutos,
        boolean activo
) {}
