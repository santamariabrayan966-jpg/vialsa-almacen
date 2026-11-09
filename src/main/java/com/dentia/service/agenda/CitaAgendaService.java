package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaCita;
import com.dentia.dto.agenda.SolicitudReservaTurnoCita;
import com.dentia.model.agenda.Cita;
import com.dentia.model.agenda.CitaResumen;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.model.agenda.EventoCita;
import com.dentia.model.agenda.TipoAtencion;
import com.dentia.model.paciente.Paciente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class CitaAgendaService {
    private static final Map<EstadoCita, EnumSet<EstadoCita>> TRANSICIONES = transiciones();

    private final ICitaAgendaDao citaDao;
    private final IPacienteDao pacienteDao;
    private final DisponibilidadService disponibilidadService;

    public CitaAgendaService(ICitaAgendaDao citaDao, IPacienteDao pacienteDao,
                             DisponibilidadService disponibilidadService) {
        this.citaDao = citaDao;
        this.pacienteDao = pacienteDao;
        this.disponibilidadService = disponibilidadService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long reservar(SolicitudReservaCita solicitud) {
        validarSolicitud(solicitud);
        Paciente paciente = pacienteDao.buscarPorId(solicitud.idPaciente())
                .orElseThrow(() -> new AgendaValidationException("El paciente no existe."));
        if (!paciente.isActivo()) throw new AgendaValidationException("El paciente esta inactivo.");
        TipoAtencion tipo = disponibilidadService.tipoActivo(solicitud.idTipoAtencion());
        LocalDateTime fin = solicitud.fechaHoraInicio().plusMinutes(tipo.duracionMinutos());

        // Las dos filas se bloquean en orden estable dentro de esta transaccion.
        // Cualquier reserva concurrente del mismo paciente o profesional/dia debe esperar.
        citaDao.bloquearReserva(solicitud.idProfesional(), solicitud.idPaciente(),
                solicitud.fechaHoraInicio().toLocalDate());
        if (!disponibilidadService.esDisponible(solicitud.idProfesional(), solicitud.idPaciente(),
                solicitud.idTipoAtencion(), solicitud.fechaHoraInicio())) {
            throw new AgendaValidationException("El horario ya no esta disponible. Selecciona otro espacio.");
        }

        String motivo = (solicitud.motivoConsulta() == null || solicitud.motivoConsulta().isBlank())
                ? "Consulta odontológica general"
                : solicitud.motivoConsulta().trim();

        Cita cita = new Cita(null, solicitud.idPaciente(), solicitud.idProfesional(), solicitud.idTipoAtencion(),
                solicitud.fechaHoraInicio(), fin, motivo, EstadoCita.RESERVADA,
                solicitud.origen(), solicitud.idCitaOrigen(), solicitud.creadoPor(), null, null);
        long idCita = citaDao.crear(cita);
        citaDao.registrarHistorial(idCita, null, EstadoCita.RESERVADA, EventoCita.CREACION,
                solicitud.creadoPor(), solicitud.origen().name().equals("PORTAL_PACIENTE")
                        ? solicitud.idPaciente() : null,
                "Cita creada desde " + solicitud.origen().name());
        return idCita;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long reservarTurno(SolicitudReservaTurnoCita solicitud) {
        if (solicitud == null || solicitud.idPaciente() == null || solicitud.idProfesional() == null
                || solicitud.idHorario() == null || solicitud.idTipoAtencion() == null
                || solicitud.fecha() == null || solicitud.origen() == null) {
            throw new AgendaValidationException("Faltan datos obligatorios para reservar el turno.");
        }
        String motivo = (solicitud.motivoConsulta() == null || solicitud.motivoConsulta().isBlank())
                ? "Consulta odontológica general"
                : solicitud.motivoConsulta().trim();
        if (motivo.length() > 1000) {
            throw new AgendaValidationException("El motivo de consulta es demasiado extenso.");
        }
        Paciente paciente = pacienteDao.buscarPorId(solicitud.idPaciente())
                .orElseThrow(() -> new AgendaValidationException("El paciente no existe."));
        if (!paciente.isActivo()) throw new AgendaValidationException("El paciente está inactivo.");
        disponibilidadService.tipoActivo(solicitud.idTipoAtencion());

        citaDao.bloquearReserva(solicitud.idProfesional(), solicitud.idPaciente(), solicitud.fecha());
        var turno = disponibilidadService.obtenerTurno(solicitud.fecha(), solicitud.idHorario(),
                solicitud.idProfesional(), solicitud.idPaciente());

        Cita cita = new Cita(null, solicitud.idPaciente(), solicitud.idProfesional(), solicitud.idTipoAtencion(),
                turno.inicio(), turno.fin(), motivo, EstadoCita.RESERVADA,
                solicitud.origen(), solicitud.idCitaOrigen(), solicitud.creadoPor(), null, null);
        long idCita = citaDao.crear(cita);
        citaDao.registrarHistorial(idCita, null, EstadoCita.RESERVADA, EventoCita.CREACION,
                solicitud.creadoPor(), null,
                "Turno reservado de " + turno.inicio().toLocalTime() + " a " + turno.fin().toLocalTime()
                        + "; atención por orden de llegada.");
        return idCita;
    }

    @Transactional
    public void cambiarEstado(long idCita, EstadoCita nuevo, Integer idUsuario, String observacion) {
        Cita cita = obtener(idCita);
        EstadoCita anterior = cita.estado();
        if (nuevo == EstadoCita.CANCELADA)
            throw new AgendaValidationException("Utiliza la accion de cancelacion e indica el motivo.");
        if (!esTransicionValida(anterior, nuevo))
            throw new AgendaValidationException("Transicion no permitida: " + anterior + " -> " + nuevo + ".");
        if (!citaDao.actualizarEstado(idCita, anterior, nuevo))
            throw new AgendaValidationException("La cita fue modificada por otro usuario. Actualiza la agenda.");
        EventoCita evento = nuevo == EstadoCita.CONFIRMADA ? EventoCita.CONFIRMACION : EventoCita.CAMBIO_ESTADO;
        citaDao.registrarHistorial(idCita, anterior, nuevo, evento, idUsuario, null, observacion);
    }

    @Transactional
    public void cancelar(long idCita, String motivo, Integer idUsuario, Long idPacienteResponsable) {
        if (motivo == null || motivo.isBlank()) throw new AgendaValidationException("Indica el motivo de cancelacion.");
        Cita cita = obtener(idCita);
        if (!esTransicionValida(cita.estado(), EstadoCita.CANCELADA))
            throw new AgendaValidationException("La cita ya no puede cancelarse desde su estado actual.");
        if (idPacienteResponsable != null && !cita.idPaciente().equals(idPacienteResponsable))
            throw new AgendaValidationException("La cita no pertenece al paciente autenticado.");
        if (!citaDao.registrarCancelacion(idCita, cita.estado(), motivo.trim(), idUsuario, idPacienteResponsable))
            throw new AgendaValidationException("La cita fue modificada por otro usuario. Actualiza la agenda.");
        citaDao.registrarHistorial(idCita, cita.estado(), EstadoCita.CANCELADA, EventoCita.CANCELACION,
                idUsuario, idPacienteResponsable, motivo.trim());
    }

    @Transactional
    public int marcarCitasVencidasComoNoAsistio() {
        return citaDao.marcarCitasVencidasComoNoAsistio(LocalDateTime.now());
    }

    public Cita obtener(long idCita) {
        Cita cita = citaDao.buscarPorId(idCita)
                .orElseThrow(() -> new AgendaValidationException("La cita no existe."));
        if ((cita.estado() == EstadoCita.RESERVADA || cita.estado() == EstadoCita.CONFIRMADA || cita.estado() == EstadoCita.EN_ESPERA)
                && cita.fechaHoraFin() != null && !cita.fechaHoraFin().isAfter(LocalDateTime.now())) {
            marcarCitasVencidasComoNoAsistio();
            cita = citaDao.buscarPorId(idCita).orElse(cita);
        }
        return cita;
    }

    public List<CitaResumen> listarAgenda(LocalDate desde, LocalDate hasta, Long profesional,
                                           EstadoCita estado, String paciente) {
        marcarCitasVencidasComoNoAsistio();
        return citaDao.listarAgenda(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), profesional, estado, paciente);
    }

    public List<CitaResumen> listarPaciente(long idPaciente) {
        marcarCitasVencidasComoNoAsistio();
        return citaDao.listarPorPaciente(idPaciente);
    }

    public boolean esTransicionValida(EstadoCita actual, EstadoCita nuevo) {
        return actual != null && nuevo != null && TRANSICIONES.getOrDefault(actual, EnumSet.noneOf(EstadoCita.class)).contains(nuevo);
    }

    private void validarSolicitud(SolicitudReservaCita s) {
        if (s == null || s.idPaciente() == null || s.idProfesional() == null || s.idTipoAtencion() == null
                || s.fechaHoraInicio() == null || s.origen() == null)
            throw new AgendaValidationException("Faltan datos obligatorios para registrar la cita.");
        if (s.motivoConsulta() != null && s.motivoConsulta().trim().length() > 1000)
            throw new AgendaValidationException("El motivo de consulta es demasiado extenso.");
    }

    private static Map<EstadoCita, EnumSet<EstadoCita>> transiciones() {
        Map<EstadoCita, EnumSet<EstadoCita>> mapa = new EnumMap<>(EstadoCita.class);
        mapa.put(EstadoCita.RESERVADA, EnumSet.of(EstadoCita.CONFIRMADA, EstadoCita.EN_ESPERA, EstadoCita.EN_ATENCION, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO));
        mapa.put(EstadoCita.CONFIRMADA, EnumSet.of(EstadoCita.EN_ESPERA, EstadoCita.EN_ATENCION, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO));
        mapa.put(EstadoCita.EN_ESPERA, EnumSet.of(EstadoCita.EN_ATENCION, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO));
        mapa.put(EstadoCita.EN_ATENCION, EnumSet.of(EstadoCita.ATENDIDA, EstadoCita.RESERVADA, EstadoCita.EN_ESPERA, EstadoCita.CANCELADA));
        mapa.put(EstadoCita.ATENDIDA, EnumSet.noneOf(EstadoCita.class));
        mapa.put(EstadoCita.CANCELADA, EnumSet.noneOf(EstadoCita.class));
        mapa.put(EstadoCita.NO_ASISTIO, EnumSet.of(EstadoCita.RESERVADA, EstadoCita.EN_ATENCION));
        return Map.copyOf(mapa);
    }
}
