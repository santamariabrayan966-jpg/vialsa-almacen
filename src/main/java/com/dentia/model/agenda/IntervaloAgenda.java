package com.dentia.model.agenda;

import java.time.LocalDateTime;

public record IntervaloAgenda(LocalDateTime inicio, LocalDateTime fin) {
    public boolean seSuperpone(LocalDateTime otroInicio, LocalDateTime otroFin) {
        return inicio.isBefore(otroFin) && fin.isAfter(otroInicio);
    }
}
