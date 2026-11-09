package com.dentia.controller.interno;

import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaCita;
import com.dentia.dto.agenda.SolicitudReservaTurnoCita;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.model.agenda.OrigenCita;
import com.dentia.model.agenda.SlotDisponible;
import com.dentia.model.agenda.TurnoDisponible;
import com.dentia.service.agenda.AgendaValidationException;
import com.dentia.service.agenda.CitaAgendaService;
import com.dentia.service.agenda.DisponibilidadService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.clinica.ServicioDentalCatalogoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/interno/citas")
public class CitaAgendaController {
    private final CitaAgendaService citaService;
    private final DisponibilidadService disponibilidadService;
    private final ProfesionalAgendaService profesionalService;
    private final IPacienteDao pacienteDao;
    private final ServicioDentalCatalogoService servicios;

    public CitaAgendaController(CitaAgendaService citaService, DisponibilidadService disponibilidadService,
                                ProfesionalAgendaService profesionalService, IPacienteDao pacienteDao,
                                ServicioDentalCatalogoService servicios) {
        this.citaService = citaService;
        this.disponibilidadService = disponibilidadService;
        this.profesionalService = profesionalService;
        this.pacienteDao = pacienteDao;
        this.servicios = servicios;
    }

    @GetMapping
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         @RequestParam(defaultValue = "dia") String vista,
                         @RequestParam(required = false) Long profesional,
                         @RequestParam(required = false) EstadoCita estado,
                         @RequestParam(required = false) String paciente, Model model) {
        LocalDate base = fecha == null ? LocalDate.now() : fecha;
        LocalDate desde = "semana".equals(vista) ? base.minusDays(base.getDayOfWeek().getValue() - 1L) : base;
        LocalDate hasta = "proximas".equals(vista) ? base.plusDays(7) : "semana".equals(vista) ? desde.plusDays(6) : base;
        model.addAttribute("citas", citaService.listarAgenda(desde, hasta, profesional, estado, paciente));
        model.addAttribute("profesionales", profesionalService.listarActivos());
        model.addAttribute("servicios", servicios.listar());
        model.addAttribute("tiposAtencion", profesionalService.listarTiposAtencion(true));
        model.addAttribute("pacientes", pacienteDao.listarActivos());
        model.addAttribute("estados", EstadoCita.values());
        model.addAttribute("fecha", base);
        model.addAttribute("vista", vista);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("diaAnterior", base.minusDays(1));
        model.addAttribute("diaSiguiente", base.plusDays(1));
        model.addAttribute("semanaAnterior", base.minusWeeks(1));
        model.addAttribute("semanaSiguiente", base.plusWeeks(1));
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("filtroProfesional", profesional);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroPaciente", paciente);
        model.addAttribute("ahora", LocalDateTime.now());
        return "interno/citas/agenda";
    }

    @GetMapping("/atender/{idCita}")
    public String atender(@PathVariable long idCita, HttpSession session, RedirectAttributes redirect) {
        try {
            var cita = citaService.obtener(idCita);
            if (cita.estado() == EstadoCita.RESERVADA || cita.estado() == EstadoCita.CONFIRMADA
                    || cita.estado() == EstadoCita.EN_ESPERA || cita.estado() == EstadoCita.NO_ASISTIO) {
                Integer usuario = (Integer) session.getAttribute("usuarioActualId");
                citaService.cambiarEstado(idCita, EstadoCita.EN_ATENCION, usuario, "Ingreso a box y apertura de expediente 360°");
            }
            return "redirect:/interno/clinica/odontograma?pacienteId=" + cita.idPaciente() + "&citaId=" + idCita;
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "No se pudo iniciar la atención: " + ex.getMessage());
            return "redirect:/interno/citas";
        }
    }

    @GetMapping("/disponibilidad")
    @ResponseBody
    public List<SlotDisponible> disponibilidad(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                @RequestParam long tipoAtencion,
                                                @RequestParam(required = false) Long profesional,
                                                @RequestParam(required = false) Long paciente) {
        return disponibilidadService.buscar(fecha, tipoAtencion, profesional, paciente);
    }

    @GetMapping("/turnos")
    @ResponseBody
    public List<TurnoDisponible> turnos(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                        @RequestParam(required = false) Long profesional,
                                        @RequestParam(required = false) Long paciente) {
        return disponibilidadService.buscarTurnos(fecha, profesional, paciente);
    }

    @GetMapping("/mes-disponibilidad")
    @ResponseBody
    public Map<String, Object> mesDisponibilidad(@RequestParam int anio, @RequestParam int mes,
                                                 @RequestParam(required = false) Long profesional) {
        return disponibilidadService.calcularMapaMes(java.time.YearMonth.of(anio, mes), profesional);
    }

    @PostMapping
    public String crear(@RequestParam long idPaciente, @RequestParam long idProfesional,
                        @RequestParam long idHorario,
                        @RequestParam(required = false) Long servicioId,
                        @RequestParam(required = false) Long idTipoAtencion,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                        @RequestParam(required = false) String motivoConsulta,
                        @RequestParam(required = false) String observaciones,
                        HttpSession session, RedirectAttributes redirect) {
        try {
            Integer usuario = (Integer) session.getAttribute("usuarioActualId");
            long tipoAgenda;
            if (idTipoAtencion != null && idTipoAtencion > 0) {
                tipoAgenda = idTipoAtencion;
            } else if (servicioId != null && servicioId > 0) {
                tipoAgenda = servicios.tipoAgendaParaServicio(servicioId);
            } else {
                tipoAgenda = servicios.tipoAgendaPorDefecto();
            }

            StringBuilder motivoFinal = new StringBuilder();
            if (motivoConsulta != null && !motivoConsulta.isBlank()) {
                motivoFinal.append(motivoConsulta.trim());
            } else {
                motivoFinal.append("Consulta odontológica general");
            }
            if (observaciones != null && !observaciones.isBlank()) {
                motivoFinal.append(" [Obs: ").append(observaciones.trim()).append("]");
            }

            citaService.reservarTurno(new SolicitudReservaTurnoCita(idPaciente, idProfesional, idHorario,
                    tipoAgenda, fecha, motivoFinal.toString(), OrigenCita.PANEL_INTERNO, usuario, null));
            redirect.addFlashAttribute("ok", "Turno reservado correctamente. La atención será por orden de llegada.");
        } catch (AgendaValidationException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/interno/citas?fecha=" + fecha;
    }

    @PostMapping("/estado")
    public String estado(@RequestParam long idCita, @RequestParam EstadoCita nuevoEstado,
                         @RequestParam(required = false) String observacion,
                         @RequestParam(required = false) String fecha,
                         HttpSession session, RedirectAttributes redirect) {
        try {
            citaService.cambiarEstado(idCita, nuevoEstado, (Integer) session.getAttribute("usuarioActualId"), observacion);
            String mensaje = switch (nuevoEstado) {
                case RESERVADA -> "Cita regresada a espera/reservada.";
                case EN_ATENCION -> "Cita en atención médica.";
                case ATENDIDA -> "Cita finalizada con éxito.";
                case NO_ASISTIO -> "Cita marcada como no asistió.";
                default -> "Estado actualizado a " + nuevoEstado + ".";
            };
            redirect.addFlashAttribute("ok", mensaje);
        } catch (AgendaValidationException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/interno/citas" + (fecha != null && !fecha.isBlank() ? "?fecha=" + fecha : "");
    }

    @PostMapping("/cancelar")
    public String cancelar(@RequestParam long idCita, @RequestParam String motivo,
                           @RequestParam(required = false) String fecha,
                           HttpSession session, RedirectAttributes redirect) {
        try {
            citaService.cancelar(idCita, motivo, (Integer) session.getAttribute("usuarioActualId"), null);
            redirect.addFlashAttribute("ok", "Cita cancelada correctamente.");
        } catch (AgendaValidationException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/interno/citas" + (fecha != null && !fecha.isBlank() ? "?fecha=" + fecha : "");
    }
}
