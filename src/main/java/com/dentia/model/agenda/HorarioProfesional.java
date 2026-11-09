package com.dentia.model.agenda;

import java.time.LocalTime;

public record HorarioProfesional(
        Long idHorario,
        Long idProfesional,
        int diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo
) {
    public String nombreDia() {
        return switch (diaSemana) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miercoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sabado";
            case 7 -> "Domingo";
            default -> "Dia no valido";
        };
    }
}
