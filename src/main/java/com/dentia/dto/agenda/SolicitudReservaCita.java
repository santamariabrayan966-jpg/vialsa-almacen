package com.dentia.dto.agenda;

import com.dentia.model.agenda.OrigenCita;

import java.time.LocalDateTime;

public record SolicitudReservaCita(
        Long idPaciente,
        Long idProfesional,
        Long idTipoAtencion,
        LocalDateTime fechaHoraInicio,
        String motivoConsulta,
        OrigenCita origen,
        Integer creadoPor,
        Long idCitaOrigen
) {}
