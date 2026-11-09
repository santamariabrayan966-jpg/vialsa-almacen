package com.dentia.model.agenda;

import java.time.LocalDateTime;

public record CitaResumen(
        Long idCita,
        Long idPaciente,
        String paciente,
        Long idProfesional,
        String profesional,
        String especialidad,
        String tipoAtencion,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoCita estado,
        OrigenCita origen,
        String motivoConsulta,
        String pacienteCelular
) {
    public CitaResumen(Long idCita, Long idPaciente, String paciente, Long idProfesional,
                       String profesional, String especialidad, String tipoAtencion,
                       LocalDateTime inicio, LocalDateTime fin, EstadoCita estado,
                       OrigenCita origen, String motivoConsulta) {
        this(idCita, idPaciente, paciente, idProfesional, profesional, especialidad,
                tipoAtencion, inicio, fin, estado, origen, motivoConsulta, null);
    }
}
