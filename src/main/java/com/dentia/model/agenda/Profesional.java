package com.dentia.model.agenda;

import java.time.LocalDateTime;
import java.util.List;

public record Profesional(
        Long idProfesional,
        String nombres,
        String apellidos,
        String documento,
        String numeroColegiatura,
        String telefono,
        String correo,
        String foto,
        boolean activo,
        Integer idUsuario,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        List<Especialidad> especialidades
) {
    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
    public boolean tieneEspecialidad(Long id) {
        return id != null && especialidades != null && especialidades.stream()
                .anyMatch(e -> id.equals(e.idEspecialidad()));
    }
    public String especialidadesTexto() {
        if (especialidades == null || especialidades.isEmpty()) return "";
        return especialidades.stream()
                .map(Especialidad::nombre)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
