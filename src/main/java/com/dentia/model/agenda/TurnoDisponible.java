package com.dentia.model.agenda;

import java.time.LocalDateTime;

/** Turno laboral reservable bajo la modalidad de atención por orden de llegada. */
public record TurnoDisponible(
        Long idHorario,
        Long idProfesional,
        String profesional,
        LocalDateTime inicio,
        LocalDateTime fin
) {}
