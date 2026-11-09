package com.dentia.model.agenda;

import java.time.LocalDateTime;

public record Cita(
        Long idCita,
        Long idPaciente,
        Long idProfesional,
        Long idTipoAtencion,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String motivoConsulta,
        EstadoCita estado,
        OrigenCita origen,
        Long idCitaOrigen,
        Integer creadoPor,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}
