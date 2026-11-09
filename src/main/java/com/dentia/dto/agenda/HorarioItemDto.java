package com.dentia.dto.agenda;

import java.time.Duration;
import java.time.LocalTime;

public record HorarioItemDto(
        long idHorario,
        long idProfesional,
        int diaSemana,
        String nombreDia,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo,
        int numeroTurnoDia,
        String duracionTexto
) {
    public static String calcularDuracion(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) return "";
        long minutos;
        if (!fin.isBefore(inicio)) {
            minutos = Duration.between(inicio, fin).toMinutes();
        } else {
            minutos = Duration.between(inicio, LocalTime.MAX).toMinutes() + 1
                    + Duration.between(LocalTime.MIN, fin).toMinutes();
        }
        long h = minutos / 60;
        long m = minutos % 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0) return h + "h";
        return m + "m";
    }
}
