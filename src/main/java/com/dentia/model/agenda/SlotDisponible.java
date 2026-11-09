package com.dentia.model.agenda;

import java.time.LocalDateTime;

public record SlotDisponible(
        Long idProfesional,
        String profesional,
        LocalDateTime inicio,
        LocalDateTime fin
) {}
