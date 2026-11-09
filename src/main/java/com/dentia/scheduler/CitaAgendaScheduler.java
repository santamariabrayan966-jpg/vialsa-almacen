package com.dentia.scheduler;

import com.dentia.service.agenda.CitaAgendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CitaAgendaScheduler {

    private static final Logger log = LoggerFactory.getLogger(CitaAgendaScheduler.class);
    private final CitaAgendaService citaAgendaService;

    public CitaAgendaScheduler(CitaAgendaService citaAgendaService) {
        this.citaAgendaService = citaAgendaService;
    }

    /**
     * Tarea programada que se ejecuta cada minuto para verificar citas
     * que hayan concluido su horario de atención y marcarlas como NO_ASISTIO.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 5000)
    public void procesarInasistenciasAutomaticas() {
        try {
            int total = citaAgendaService.marcarCitasVencidasComoNoAsistio();
            if (total > 0) {
                log.info("Inasistencias automáticas actualizadas: {} cita(s) marcada(s) como NO_ASISTIO.", total);
            }
        } catch (Exception ex) {
            log.error("Error al procesar inasistencias automáticas de citas", ex);
        }
    }
}
