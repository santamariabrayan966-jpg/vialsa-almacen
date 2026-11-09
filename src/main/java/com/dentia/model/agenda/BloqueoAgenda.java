package com.dentia.model.agenda;

import java.time.LocalDateTime;

public record BloqueoAgenda(
        Long idBloqueo,
        Long idProfesional,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String motivo,
        String observacion,
        Integer creadoPor,
        LocalDateTime fechaCreacion
) {}
