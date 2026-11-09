package com.dentia.controller.interno;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.agenda.Profesional;
import com.dentia.model.interno.Usuario;
import com.dentia.service.agenda.AgendaValidationException;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.shared.FileStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dentia.dto.agenda.HorarioItemDto;
import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.HorarioProfesional;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/interno/profesionales")
public class ProfesionalAgendaController {

    private final ProfesionalAgendaService service;
    private final UsuarioDao usuarios;
    private final FileStorageService archivos;

    public ProfesionalAgendaController(ProfesionalAgendaService s, UsuarioDao u, FileStorageService a) {
        this.service = s;
        this.usuarios = u;
        this.archivos = a;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Long especialidad,
                         @RequestParam(required = false) Boolean activo,
                         Model m) {
        List<Profesional> lista = service.buscarProfesionales(q, especialidad, activo);
        List<Especialidad> listaEsp = service.listarEspecialidades(false);
        List<Profesional> todos = service.buscarProfesionales(null, null, null);

        Map<Long, Long> conteo = listaEsp.stream()
                .collect(Collectors.toMap(
                        Especialidad::idEspecialidad,
                        esp -> todos.stream().filter(p -> p.tieneEspecialidad(esp.idEspecialidad())).count()
                ));

        m.addAttribute("profesionales", lista);
        m.addAttribute("especialidades", listaEsp);
        m.addAttribute("conteoDoctores", conteo);
        m.addAttribute("usuariosInternos", usuarios.listarUsuariosInternos());
        m.addAttribute("profesionalNuevo", new Profesional(null, "", "", null, null, null, null, null, true, null, null, null, List.of()));
        m.addAttribute("q", q);
        m.addAttribute("especialidadFiltro", especialidad);
        m.addAttribute("activoFiltro", activo);
        return "interno/profesionales/list";
    }

    @GetMapping(value = "/{id}/datos", produces = "application/json")
    @ResponseBody
    public Profesional obtenerDatos(@PathVariable long id) {
        return service.buscar(id);
    }

    @GetMapping("/nuevo")
    public String nuevo(Model m) {
        return "redirect:/interno/profesionales?abrir=nuevo";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model m) {
        return "redirect:/interno/profesionales?editar=" + id;
    }

    @GetMapping("/{id}/agenda")
    public String agenda(@PathVariable long id,
                         @RequestParam(defaultValue = "false") boolean fragment,
                         Model m) {
        m.addAttribute("profesional", service.buscar(id));

        List<HorarioProfesional> rawHorarios = service.listarHorarios(id);
        List<HorarioProfesional> sorted = rawHorarios.stream()
                .sorted(Comparator.comparingInt(HorarioProfesional::diaSemana)
                        .thenComparing(HorarioProfesional::horaInicio))
                .toList();

        List<HorarioItemDto> horariosDto = new ArrayList<>();
        int ultimoDia = -1;
        int turnoDia = 0;
        for (HorarioProfesional h : sorted) {
            if (h.diaSemana() != ultimoDia) {
                ultimoDia = h.diaSemana();
                turnoDia = 1;
            } else {
                turnoDia++;
            }
            String nombreDia = DayOfWeek.of(h.diaSemana())
                    .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            if (nombreDia != null && !nombreDia.isEmpty()) {
                nombreDia = Character.toUpperCase(nombreDia.charAt(0)) + nombreDia.substring(1);
            }
            String duracion = HorarioItemDto.calcularDuracion(h.horaInicio(), h.horaFin());
            horariosDto.add(new HorarioItemDto(h.idHorario(), h.idProfesional(), h.diaSemana(),
                    nombreDia, h.horaInicio(), h.horaFin(), h.activo(), turnoDia, duracion));
        }

        m.addAttribute("horarios", horariosDto);
        m.addAttribute("bloqueos", service.listarBloqueos(id, LocalDateTime.now().minusMonths(1), LocalDateTime.now().plusYears(2)));
        m.addAttribute("motivosBloqueo", service.motivosBloqueo());
        m.addAttribute("fragment", fragment);
        return fragment ? "interno/profesionales/agenda :: agendaContent" : "interno/profesionales/agenda";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long idProfesional,
                          @RequestParam String nombres,
                          @RequestParam String apellidos,
                          @RequestParam(required = false) String documento,
                          @RequestParam(required = false) String numeroColegiatura,
                          @RequestParam(required = false) String telefono,
                          @RequestParam(required = false) String correo,
                          @RequestParam(required = false) String foto,
                          @RequestParam(required = false) MultipartFile fotoFile,
                          @RequestParam(defaultValue = "false") boolean eliminarFoto,
                          @RequestParam(required = false) Integer idUsuario,
                          @RequestParam(required = false) List<Long> especialidades,
                          RedirectAttributes ra) {
        try {
            String fotoFinal = foto;
            if (!eliminarFoto && idProfesional != null && (fotoFinal == null || fotoFinal.isBlank())) {
                try {
                    fotoFinal = service.buscar(idProfesional).foto();
                } catch (Exception ignored) {}
            }

            if (eliminarFoto) fotoFinal = null;
            if (fotoFile != null && !fotoFile.isEmpty()) {
                fotoFinal = archivos.guardarFotoProfesional(fotoFile);
            }

            // Una cuenta interna vinculada representa el mismo profesional.
            // El formulario ya copia estos datos en el navegador, pero se repite
            // aquí para que el vínculo también sea consistente vía API o scripts.
            String nombresFinal = nombres != null ? nombres.trim().toUpperCase() : null;
            String apellidosFinal = apellidos != null ? apellidos.trim().toUpperCase() : null;
            String documentoFinal = documento;
            String telefonoFinal = telefono;
            String correoFinal = correo;
            String colegiaturaFinal = numeroColegiatura;
            if (idUsuario != null) {
                Usuario cuenta = usuarios.obtenerPorId(idUsuario);
                if (cuenta == null) {
                    throw new AgendaValidationException("La cuenta interna seleccionada no existe.");
                }
                nombresFinal = preferido(cuenta.getNombres(), nombresFinal);
                apellidosFinal = preferido(cuenta.getApellidos(), apellidosFinal);
                documentoFinal = preferido(cuenta.getNroDocumento(), documentoFinal);
                telefonoFinal = preferido(cuenta.getTelefono(), telefonoFinal);
                correoFinal = preferido(cuenta.getCorreo(), correoFinal);
            }
            if (colegiaturaFinal == null || colegiaturaFinal.isBlank()) {
                try {
                    colegiaturaFinal = idProfesional == null ? null : service.buscar(idProfesional).numeroColegiatura();
                } catch (Exception ignored) { }
            }

            long id = service.guardarProfesional(
                    new Profesional(idProfesional, nombresFinal, apellidosFinal, documentoFinal, colegiaturaFinal, telefonoFinal, correoFinal, fotoFinal, true, idUsuario, null, null, List.of()),
                    especialidades
            );
            ra.addFlashAttribute("ok", "Profesional guardado correctamente.");
            return "redirect:/interno/profesionales";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", mensaje(ex));
            ra.addFlashAttribute("abrirModalProfesional", true);
            return "redirect:/interno/profesionales";
        }
    }

    @PostMapping("/{id}/estado")
    public String estado(@PathVariable long id, @RequestParam boolean activo, RedirectAttributes ra) {
        try {
            service.cambiarEstado(id, activo);
            ra.addFlashAttribute("ok", "Estado del profesional actualizado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", mensaje(ex));
        }
        return "redirect:/interno/profesionales";
    }

    @PostMapping("/{id}/horarios")
    public String horario(@PathVariable long id,
                          @RequestParam(required = false) Long idHorario,
                          @RequestParam int diaSemana,
                          @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
                          @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
                          @RequestParam(defaultValue = "false") boolean fragment,
                          RedirectAttributes ra) {
        try {
            service.guardarHorario(idHorario, id, diaSemana, horaInicio, horaFin);
            ra.addFlashAttribute("ok", "Horario guardado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", mensaje(ex));
        }
        return redirectAgenda(id, fragment);
    }

    @PostMapping("/{id}/horarios/{horario}/eliminar")
    public String eliminarHorario(@PathVariable long id, @PathVariable long horario,
                                  @RequestParam(defaultValue = "false") boolean fragment) {
        service.eliminarHorario(id, horario);
        return redirectAgenda(id, fragment);
    }

    @PostMapping("/{id}/bloqueos")
    public String bloqueo(@PathVariable long id,
                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime inicio,
                          @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fin,
                          @RequestParam String motivo,
                          @RequestParam(required = false) String observacion,
                          @RequestParam(defaultValue = "false") boolean fragment,
                          HttpSession session,
                          RedirectAttributes ra) {
        try {
            Integer usuario = (Integer) session.getAttribute("usuarioActualId");
            if (usuario == null) throw new AgendaValidationException("No se identificó al usuario interno.");
            service.crearBloqueo(id, inicio, fin, motivo, observacion, usuario);
            ra.addFlashAttribute("ok", "Bloqueo registrado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", mensaje(ex));
        }
        return redirectAgenda(id, fragment);
    }

    @PostMapping("/{id}/bloqueos/{bloqueo}/eliminar")
    public String eliminarBloqueo(@PathVariable long id, @PathVariable long bloqueo,
                                  @RequestParam(defaultValue = "false") boolean fragment) {
        service.eliminarBloqueo(id, bloqueo);
        return redirectAgenda(id, fragment);
    }

    private String form(Profesional p, String titulo, Model m) {
        m.addAttribute("profesional", p);
        m.addAttribute("titulo", titulo);
        m.addAttribute("especialidades", service.listarEspecialidades(true));
        m.addAttribute("usuariosInternos", usuarios.listarUsuariosInternos());
        return "interno/profesionales/form";
    }

    private String mensaje(RuntimeException ex) {
        return ex.getMessage() == null ? "No se pudo completar la operación." : ex.getMessage();
    }

    private String preferido(String cuenta, String manual) {
        return cuenta == null || cuenta.isBlank() ? manual : cuenta;
    }

    private String redirectAgenda(long id, boolean fragment) {
        return "redirect:/interno/profesionales/" + id + "/agenda" + (fragment ? "?fragment=true" : "");
    }
}
