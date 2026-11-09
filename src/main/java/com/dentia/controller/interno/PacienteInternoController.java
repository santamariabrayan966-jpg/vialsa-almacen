package com.dentia.controller.interno;

import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.model.agenda.CitaResumen;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.model.paciente.Paciente;
import com.dentia.service.interno.PacienteInternoService;
import com.dentia.service.interno.ClinicaInternaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/interno/pacientes")
public class PacienteInternoController {

    private final PacienteInternoService service;
    private final ICitaAgendaDao citas;
    private final ClinicaInternaService clinicaService;

    public PacienteInternoController(PacienteInternoService service, ICitaAgendaDao citas) {
        this(service, citas, null);
    }

    @Autowired
    public PacienteInternoController(PacienteInternoService service, ICitaAgendaDao citas, ClinicaInternaService clinicaService) {
        this.service = service;
        this.citas = citas;
        this.clinicaService = clinicaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Boolean activo,
                         @RequestParam(required = false) Boolean nuevo,
                         @RequestParam(required = false) Boolean atendidosHoy,
                         @RequestParam(required = false) Boolean mios,
                         org.springframework.security.core.Authentication auth,
                         @RequestParam(required = false) Long editar,
                         Model model) {
        var pacientes = service.buscar(q, activo);
        if (Boolean.TRUE.equals(atendidosHoy) && clinicaService != null) {
            var ids = clinicaService.pacientesAtendidosHoy();
            pacientes = pacientes.stream().filter(p -> ids.contains(p.getIdPaciente())).toList();
        }
        if (Boolean.TRUE.equals(mios) && clinicaService != null) {
            var ids = clinicaService.misPacientes(auth.getName());
            pacientes = pacientes.stream().filter(p -> ids.contains(p.getIdPaciente())).toList();
        }
        model.addAttribute("mios", Boolean.TRUE.equals(mios));
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("atendidosHoy", Boolean.TRUE.equals(atendidosHoy));
        model.addAttribute("q", q);
        model.addAttribute("activoFiltro", activo);

        if (!model.containsAttribute("pacienteFormulario")) {
            model.addAttribute("pacienteFormulario", editar == null ? new Paciente() : service.obtener(editar));
        }
        if (!model.containsAttribute("abrirModalPaciente")) {
            model.addAttribute("abrirModalPaciente", Boolean.TRUE.equals(nuevo) || editar != null);
        }
        return "interno/pacientes/list";
    }

    @GetMapping("/mis-atenciones")
    public String misAtenciones(org.springframework.security.core.Authentication auth, Model model) {
        model.addAttribute("atenciones", clinicaService.misAtenciones(auth.getName()));
        return "interno/pacientes/mis-atenciones";
    }

    @GetMapping("/nuevo")
    public String nuevo() {
        return "redirect:/interno/pacientes?nuevo=true";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id) {
        return "redirect:/interno/pacientes?editar=" + id;
    }

    @PostMapping
    public String guardar(@RequestParam(required = false) Long idPaciente,
                          @RequestParam(required = false) String dni,
                          @RequestParam(required = false) String nombres,
                          @RequestParam(required = false) String apellidos,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
                          @RequestParam(required = false) String direccion,
                          @RequestParam(required = false) String celular,
                          @RequestParam(required = false) String correo,
                          RedirectAttributes redirect) {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(idPaciente);
        paciente.setDni(dni != null ? dni.trim() : null);
        paciente.setNombres(nombres != null ? nombres.trim().toUpperCase() : null);
        paciente.setApellidos(apellidos != null ? apellidos.trim().toUpperCase() : null);
        paciente.setFechaNacimiento(fechaNacimiento);
        paciente.setDireccion(direccion != null ? direccion.trim().toUpperCase() : null);
        paciente.setCelular(celular != null ? celular.trim() : null);
        paciente.setCorreo(correo != null ? correo.trim().toLowerCase() : null);

        try {
            service.guardar(paciente);
            redirect.addFlashAttribute("ok", idPaciente == null
                    ? "Paciente registrado correctamente."
                    : "Datos del paciente actualizados.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("pacienteFormulario", paciente);
            redirect.addFlashAttribute("abrirModalPaciente", true);
        }
        return "redirect:/interno/pacientes";
    }

    @PostMapping("/{id}/estado")
    public String estado(@PathVariable long id, @RequestParam boolean activo) {
        service.cambiarEstado(id, activo);
        return "redirect:/interno/pacientes";
    }

    @GetMapping("/{id}")
    public String ficha(@PathVariable long id, Model model) {
        Paciente paciente = service.obtener(id);
        List<CitaResumen> todas = citas.listarPorPaciente(id);
        LocalDateTime ahora = LocalDateTime.now();
        List<CitaResumen> futuras = todas.stream()
                .filter(cita -> cita.inicio().isAfter(ahora) && cita.estado() != EstadoCita.CANCELADA)
                .sorted(Comparator.comparing(CitaResumen::inicio))
                .toList();
        CitaResumen ultima = todas.stream()
                .filter(cita -> cita.inicio().isBefore(ahora))
                .max(Comparator.comparing(CitaResumen::inicio))
                .orElse(null);

        List<CitaResumen> historialCitas = todas.stream()
                .sorted(Comparator.comparing(CitaResumen::inicio).reversed())
                .toList();

        Map<String, Object> expediente = null;
        if (clinicaService != null) {
            try {
                expediente = clinicaService.cargarExpedienteCompleto(id);
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("paciente", paciente);
        model.addAttribute("citasFuturas", futuras);
        model.addAttribute("proximaCita", futuras.isEmpty() ? null : futuras.get(0));
        model.addAttribute("ultimaCita", ultima);
        model.addAttribute("todasLasCitas", historialCitas);
        model.addAttribute("expediente", expediente);
        return "interno/pacientes/ficha";
    }
}
