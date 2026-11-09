package com.dentia.dto.agenda;

import com.dentia.model.agenda.OrigenCita;

import java.time.LocalDate;

public record SolicitudReservaTurnoCita(
        Long idPaciente,
        Long idProfesional,
        Long idHorario,
        Long idTipoAtencion,
        LocalDate fecha,
        String motivoConsulta,
        OrigenCita origen,
        Integer creadoPor,
        Long idCitaOrigen
) {}
