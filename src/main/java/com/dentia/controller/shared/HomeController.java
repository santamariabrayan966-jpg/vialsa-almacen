package com.dentia.controller.shared;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaTurnoCita;
import com.dentia.model.agenda.OrigenCita;
import com.dentia.model.agenda.TurnoDisponible;
import com.dentia.model.paciente.CuentaPaciente;
import com.dentia.service.agenda.AgendaValidationException;
import com.dentia.service.agenda.CitaAgendaService;
import com.dentia.service.agenda.DisponibilidadService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.clinica.ServicioDentalCatalogoService;
import com.dentia.service.interno.DentiaDashboardService;
import com.dentia.security.GoogleOAuthAvailability;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final DentiaDashboardService dashboardService;
    private final ProfesionalAgendaService profesionalService;
    private final DisponibilidadService disponibilidadService;
    private final CitaAgendaService citaService;
    private final ICuentaPacienteDao cuentaDao;
    private final GoogleOAuthAvailability googleOAuthAvailability;
    private final ServicioDentalCatalogoService servicios;

    @Autowired
    public HomeController(DentiaDashboardService dashboardService,
                          ProfesionalAgendaService profesionalService,
                          DisponibilidadService disponibilidadService,
                          CitaAgendaService citaService,
                          ICuentaPacienteDao cuentaDao,
                          GoogleOAuthAvailability googleOAuthAvailability,
                          ServicioDentalCatalogoService servicios) {
        this.dashboardService = dashboardService;
        this.profesionalService = profesionalService;
        this.disponibilidadService = disponibilidadService;
        this.citaService = citaService;
        this.cuentaDao = cuentaDao;
        this.googleOAuthAvailability = googleOAuthAvailability;
        this.servicios = servicios;
    }

    /** Mantiene compatibles las pruebas y los puntos de entrada anteriores. */
    public HomeController(DentiaDashboardService dashboardService,
                          ProfesionalAgendaService profesionalService,
                          DisponibilidadService disponibilidadService,
                          CitaAgendaService citaService,
                          ICuentaPacienteDao cuentaDao,
                          GoogleOAuthAvailability googleOAuthAvailability) {
        this(dashboardService, profesionalService, disponibilidadService, citaService,
                cuentaDao, googleOAuthAvailability, null);
    }

    @GetMapping("/")
    public String root(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean esPaciente = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_PACIENTE".equals(a.getAuthority()));
            return esPaciente ? "redirect:/paciente" : "redirect:/interno/dashboard";
        }
        return "redirect:/interno/login";
    }

    @GetMapping("/disponibilidad")
    @ResponseBody
    public List<TurnoDisponible> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam long tipoAtencion,
            @RequestParam(required = false) Long profesional,
            Authentication auth) {
        Long idPaciente = null;
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            idPaciente = cuentaDao.buscarPorCorreo(auth.getName())
                    .filter(CuentaPaciente::isActivo)
                    .map(CuentaPaciente::getIdPaciente)
                    .orElse(null);
        }
        if (servicios != null) servicios.validarActivo(tipoAtencion);
        else disponibilidadService.tipoActivo(tipoAtencion);
        return disponibilidadService.buscarTurnos(fecha, profesional, idPaciente);
    }

    @PostMapping("/reservar-cita")
    public String confirmarReserva(
            @RequestParam long idProfesional,
            @RequestParam long idHorario,
            @RequestParam long idTipoAtencion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "Consulta general") String motivoConsulta,
            Authentication auth,
            RedirectAttributes redirect) {

        if (auth == null || !auth.isAuthenticated() || (auth instanceof AnonymousAuthenticationToken)) {
            redirect.addFlashAttribute("errorReserva", "Debes acceder con tu cuenta de Google para confirmar tu reserva.");
            return "redirect:/?loginGoogleRequerido#reservar-cita";
        }

        Long idPaciente = cuentaDao.buscarPorCorreo(auth.getName())
                .filter(CuentaPaciente::isActivo)
                .map(CuentaPaciente::getIdPaciente)
                .orElse(null);

        if (idPaciente == null) {
            redirect.addFlashAttribute("errorReserva", "No se encontró el registro de paciente para tu cuenta Google.");
            return "redirect:/?errorReserva#reservar-cita";
        }

        try {
            long tipoAgenda = servicios == null ? idTipoAtencion : servicios.tipoAgendaParaServicio(idTipoAtencion);
            citaService.reservarTurno(new SolicitudReservaTurnoCita(
                    idPaciente,
                    idProfesional,
                    idHorario,
                    tipoAgenda,
                    fecha,
                    motivoConsulta,
                    OrigenCita.PORTAL_PACIENTE,
                    null,
                    null
            ));
            redirect.addFlashAttribute("citaReservadaOk", true);
            return "redirect:/?citaReservada=true#reservar-cita";
        } catch (AgendaValidationException ex) {
            redirect.addFlashAttribute("errorReserva", ex.getMessage());
            return "redirect:/?errorReserva=" + ex.getMessage() + "#reservar-cita";
        }
    }

    @GetMapping("/interno/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("titulo", "DentIA | Dashboard");
        model.addAttribute("resumenDashboard", dashboardService.obtenerResumen());
        model.addAttribute("agendaHoy", dashboardService.obtenerAgendaHoy());
        return "interno/dashboard";
    }
}
