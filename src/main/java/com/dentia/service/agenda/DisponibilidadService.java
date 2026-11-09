package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICatalogoAgendaDao;
import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.dao.interfaces.IProfesionalAgendaDao;
import com.dentia.model.agenda.BloqueoAgenda;
import com.dentia.model.agenda.HorarioProfesional;
import com.dentia.model.agenda.IntervaloAgenda;
import com.dentia.model.agenda.Profesional;
import com.dentia.model.agenda.SlotDisponible;
import com.dentia.model.agenda.TipoAtencion;
import com.dentia.model.agenda.TurnoDisponible;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DisponibilidadService {
    private final IProfesionalAgendaDao profesionalDao;
    private final ICatalogoAgendaDao catalogoDao;
    private final ICitaAgendaDao citaDao;
    private final int intervaloMinutos;
    private final Clock clock;

    @Autowired
    public DisponibilidadService(IProfesionalAgendaDao profesionalDao, ICatalogoAgendaDao catalogoDao,
                                 ICitaAgendaDao citaDao,
                                 @Value("${dentia.agenda.intervalo-minutos:10}") int intervaloMinutos) {
        this(profesionalDao, catalogoDao, citaDao, intervaloMinutos, Clock.systemDefaultZone());
    }

    DisponibilidadService(IProfesionalAgendaDao profesionalDao, ICatalogoAgendaDao catalogoDao,
                           ICitaAgendaDao citaDao, int intervaloMinutos, Clock clock) {
        this.profesionalDao = profesionalDao;
        this.catalogoDao = catalogoDao;
        this.citaDao = citaDao;
        this.intervaloMinutos = Math.max(5, intervaloMinutos);
        this.clock = clock;
    }

    public List<SlotDisponible> buscar(LocalDate fecha, long idTipoAtencion,
                                       Long idProfesional, Long idPaciente) {
        if (fecha == null) throw new AgendaValidationException("Selecciona una fecha.");
        TipoAtencion tipo = tipoActivo(idTipoAtencion);
        List<Profesional> profesionales = idProfesional == null
                ? profesionalDao.listarActivos()
                : List.of(profesionalActivo(idProfesional));
        List<SlotDisponible> resultado = new ArrayList<>();
        for (Profesional profesional : profesionales) {
            resultado.addAll(buscarParaProfesional(fecha, tipo, profesional, idPaciente));
        }
        return resultado.stream()
                .sorted(Comparator.comparing(SlotDisponible::inicio).thenComparing(SlotDisponible::profesional))
                .toList();
    }

    /**
     * Devuelve los bloques laborales del profesional, no horas fraccionadas.
     * Varias personas pueden reservar el mismo turno y se atienden por orden de llegada.
     */
    public List<TurnoDisponible> buscarTurnos(LocalDate fecha, Long idProfesional, Long idPaciente) {
        if (fecha == null) throw new AgendaValidationException("Selecciona una fecha.");
        List<Profesional> profesionales = idProfesional == null
                ? profesionalDao.listarActivos()
                : List.of(profesionalActivo(idProfesional));
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<TurnoDisponible> turnos = new ArrayList<>();
        for (Profesional profesional : profesionales) {
            List<HorarioProfesional> horarios = profesionalDao.listarHorariosActivos(
                    profesional.idProfesional(), fecha.getDayOfWeek().getValue());
            for (HorarioProfesional horario : horarios) {
                LocalDateTime inicio = fecha.atTime(horario.horaInicio());
                LocalDateTime fin = fecha.atTime(horario.horaFin());
                if (!fin.isAfter(ahora)) continue;
                if (!libre(profesionalDao.listarBloqueos(profesional.idProfesional(), inicio, fin), inicio, fin)) continue;
                if (idPaciente != null && citaDao.existeSuperposicionPaciente(idPaciente, inicio, fin)) continue;
                turnos.add(new TurnoDisponible(horario.idHorario(), profesional.idProfesional(),
                        profesional.nombreCompleto(), inicio, fin));
            }
        }
        return turnos.stream()
                .sorted(Comparator.comparing(TurnoDisponible::inicio).thenComparing(TurnoDisponible::profesional))
                .toList();
    }

    public TurnoDisponible obtenerTurno(LocalDate fecha, long idHorario, long idProfesional, long idPaciente) {
        return buscarTurnos(fecha, idProfesional, idPaciente).stream()
                .filter(t -> t.idHorario().equals(idHorario))
                .findFirst()
                .orElseThrow(() -> new AgendaValidationException(
                        "El turno ya no está disponible para ese paciente. Selecciona otro turno."));
    }

    public Map<String, Object> calcularMapaMes(java.time.YearMonth yearMonth, Long idProfesional) {
        LocalDate hoy = LocalDate.now(clock);
        List<Profesional> profesionales = idProfesional == null
                ? profesionalDao.listarActivos()
                : List.of(profesionalActivo(idProfesional));

        Map<String, Map<String, Object>> dias = new HashMap<>();

        for (int dia = 1; dia <= yearMonth.lengthOfMonth(); dia++) {
            LocalDate fecha = yearMonth.atDay(dia);
            String fechaIso = fecha.toString();

            if (fecha.isBefore(hoy)) {
                dias.put(fechaIso, Map.of("estado", "PASADO"));
                continue;
            }

            int diaSemana = fecha.getDayOfWeek().getValue();
            boolean tieneHorario = false;
            boolean tieneBloqueo = false;
            String motivoBloqueo = null;
            int totalTurnos = 0;

            for (Profesional p : profesionales) {
                LocalDateTime diaInicio = fecha.atStartOfDay();
                LocalDateTime diaFin = fecha.plusDays(1).atStartOfDay();
                List<BloqueoAgenda> bloqueos = profesionalDao.listarBloqueos(p.idProfesional(), diaInicio, diaFin);

                if (!bloqueos.isEmpty()) {
                    tieneBloqueo = true;
                    if (motivoBloqueo == null) {
                        motivoBloqueo = bloqueos.get(0).motivo();
                    }
                }

                List<HorarioProfesional> horarios = profesionalDao.listarHorariosActivos(p.idProfesional(), diaSemana);
                if (horarios.isEmpty()) continue;
                tieneHorario = true;

                for (HorarioProfesional h : horarios) {
                    LocalDateTime turnoInicio = fecha.atTime(h.horaInicio());
                    LocalDateTime turnoFin = fecha.atTime(h.horaFin());
                    if (turnoFin.isBefore(LocalDateTime.now(clock))) continue;
                    if (libre(bloqueos, turnoInicio, turnoFin)) {
                        totalTurnos++;
                    }
                }
            }

            if (totalTurnos > 0) {
                dias.put(fechaIso, Map.of("estado", "DISPONIBLE", "turnos", totalTurnos));
            } else if (tieneBloqueo) {
                dias.put(fechaIso, Map.of("estado", "BLOQUEADO", "motivo", motivoBloqueo != null ? motivoBloqueo : "Vacaciones / Bloqueado"));
            } else {
                dias.put(fechaIso, Map.of("estado", "NO_LABORABLE"));
            }
        }

        return Map.of(
                "anio", yearMonth.getYear(),
                "mes", yearMonth.getMonthValue(),
                "dias", dias
        );
    }

    private List<SlotDisponible> buscarParaProfesional(LocalDate fecha, TipoAtencion tipo,
                                                        Profesional profesional, Long idPaciente) {
        LocalDateTime diaInicio = fecha.atStartOfDay();
        LocalDateTime diaFin = fecha.plusDays(1).atStartOfDay();
        List<HorarioProfesional> horarios = profesionalDao.listarHorariosActivos(
                profesional.idProfesional(), fecha.getDayOfWeek().getValue());
        List<BloqueoAgenda> bloqueos = profesionalDao.listarBloqueos(profesional.idProfesional(), diaInicio, diaFin);
        List<IntervaloAgenda> ocupacionProfesional = citaDao.listarOcupacionProfesional(
                profesional.idProfesional(), diaInicio, diaFin);
        List<IntervaloAgenda> ocupacionPaciente = idPaciente == null ? List.of()
                : citaDao.listarOcupacionPaciente(idPaciente, diaInicio, diaFin);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<SlotDisponible> slots = new ArrayList<>();
        for (HorarioProfesional horario : horarios) {
            LocalDateTime cursor = fecha.atTime(horario.horaInicio());
            LocalDateTime limite = fecha.atTime(horario.horaFin());
            while (!cursor.plusMinutes(tipo.duracionMinutos()).isAfter(limite)) {
                LocalDateTime fin = cursor.plusMinutes(tipo.duracionMinutos());
                if (!cursor.isBefore(ahora)
                        && libre(bloqueos, cursor, fin)
                        && libreIntervalos(ocupacionProfesional, cursor, fin)
                        && libreIntervalos(ocupacionPaciente, cursor, fin)) {
                    slots.add(new SlotDisponible(profesional.idProfesional(), profesional.nombreCompleto(), cursor, fin));
                }
                cursor = cursor.plusMinutes(intervaloMinutos);
            }
        }
        return slots;
    }

    public boolean esDisponible(long idProfesional, long idPaciente, long idTipoAtencion, LocalDateTime inicio) {
        TipoAtencion tipo = tipoActivo(idTipoAtencion);
        Profesional profesional = profesionalActivo(idProfesional);
        LocalDateTime fin = inicio.plusMinutes(tipo.duracionMinutos());
        if (inicio.isBefore(LocalDateTime.now(clock))) return false;
        boolean dentroHorario = profesionalDao.listarHorariosActivos(idProfesional, inicio.getDayOfWeek().getValue())
                .stream().anyMatch(h -> !inicio.toLocalTime().isBefore(h.horaInicio())
                        && !fin.toLocalTime().isAfter(h.horaFin())
                        && inicio.toLocalDate().equals(fin.toLocalDate()));
        if (!dentroHorario) return false;
        if (!libre(profesionalDao.listarBloqueos(idProfesional, inicio, fin), inicio, fin)) return false;
        return !citaDao.existeSuperposicionProfesional(profesional.idProfesional(), inicio, fin)
                && !citaDao.existeSuperposicionPaciente(idPaciente, inicio, fin);
    }

    public TipoAtencion tipoActivo(long id) {
        TipoAtencion tipo = catalogoDao.buscarTipoAtencion(id)
                .orElseThrow(() -> new AgendaValidationException("El tipo de atencion no existe."));
        if (!tipo.activo()) throw new AgendaValidationException("El tipo de atencion esta inactivo.");
        return tipo;
    }

    private Profesional profesionalActivo(long id) {
        Profesional p = profesionalDao.buscarPorId(id)
                .orElseThrow(() -> new AgendaValidationException("El profesional no existe."));
        if (!p.activo()) throw new AgendaValidationException("El profesional esta inactivo.");
        return p;
    }

    private boolean libre(List<BloqueoAgenda> bloqueos, LocalDateTime inicio, LocalDateTime fin) {
        return bloqueos.stream().noneMatch(b -> b.fechaHoraInicio().isBefore(fin) && b.fechaHoraFin().isAfter(inicio));
    }

    private boolean libreIntervalos(List<IntervaloAgenda> intervalos, LocalDateTime inicio, LocalDateTime fin) {
        return intervalos.stream().noneMatch(i -> i.seSuperpone(inicio, fin));
    }
}
