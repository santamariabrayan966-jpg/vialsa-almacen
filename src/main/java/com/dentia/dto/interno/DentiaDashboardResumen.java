package com.dentia.dto.interno;

public record DentiaDashboardResumen(
        long pacientes,
        long citasHoy,
        long enEspera,
        long atendidosHoy,
        long proximasCitas,
        long profesionalesActivos
) {
}
