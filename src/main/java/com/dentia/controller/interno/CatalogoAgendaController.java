package com.dentia.controller.interno;

import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.Profesional;
import com.dentia.model.agenda.TipoAtencion;
import com.dentia.service.agenda.AgendaValidationException;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.clinica.ServicioDentalCatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Controller
public class CatalogoAgendaController {

    private final ProfesionalAgendaService service;
    private final ServicioDentalCatalogoService servicios;

    public CatalogoAgendaController(ProfesionalAgendaService service, ServicioDentalCatalogoService servicios) {
        this.service = service;
        this.servicios = servicios;
    }

    @GetMapping("/interno/especialidades")
    public String especialidades() {
        return "redirect:/interno/profesionales";
    }

    @PostMapping("/interno/especialidades")
    public String guardarEspecialidad(@RequestParam(required = false) Long idEspecialidad,
                                      @RequestParam String nombre,
                                      @RequestParam(required = false) String descripcion,
                                      RedirectAttributes ra) {
        try {
            String nombreFinal = nombre != null ? nombre.trim().toUpperCase() : null;
            if (nombreFinal == null || nombreFinal.length() < 3) {
                throw new AgendaValidationException("El nombre de la especialidad debe tener al menos 3 caracteres.");
            }
            service.guardarEspecialidad(new Especialidad(idEspecialidad, nombreFinal, descripcion != null ? descripcion.trim() : null, true));
            ra.addFlashAttribute("ok", "Especialidad guardada correctamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/interno/profesionales";
    }

    @PostMapping(value = "/interno/especialidades/ajax", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> guardarEspecialidadAjax(@RequestParam(required = false) Long idEspecialidad,
                                                     @RequestParam String nombre,
                                                     @RequestParam(required = false) String descripcion) {
        try {
            String nombreFinal = nombre != null ? nombre.trim().toUpperCase() : null;
            if (nombreFinal == null || nombreFinal.length() < 3) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la especialidad debe tener al menos 3 caracteres."));
            }
            long id = service.guardarEspecialidad(new Especialidad(idEspecialidad, nombreFinal, descripcion != null ? descripcion.trim() : null, true));
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "nombre", nombreFinal,
                    "descripcion", descripcion != null ? descripcion.trim() : "",
                    "activo", true
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/interno/especialidades/{id}/estado")
    public String estadoEspecialidad(@PathVariable long id, @RequestParam boolean activo, RedirectAttributes ra) {
        try {
            service.cambiarEstadoEspecialidad(id, activo);
            ra.addFlashAttribute("ok", "Estado de la especialidad actualizado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/interno/profesionales";
    }

    @GetMapping({"/interno/tipos-atencion", "/interno/tipos_atencion"})
    public String tipos(@RequestParam(required = false) String cat, Model m) {
        List<Map<String, Object>> lista = servicios.listar();
        List<Map<String, Object>> inactivos = servicios.listarInactivos();
        Map<String, Map<String, List<Map<String, Object>>>> grupos = new LinkedHashMap<>();
        Map<String, Long> conteos = new LinkedHashMap<>();

        for (Map<String, Object> item : lista) {
            String categoria = (String) item.get("categoria");
            String subcategoria = (String) item.get("subcategoria");
            grupos.computeIfAbsent(categoria, key -> new LinkedHashMap<>())
                    .computeIfAbsent(subcategoria, key -> new ArrayList<>()).add(item);
            conteos.put(categoria, conteos.getOrDefault(categoria, 0L) + 1L);
        }

        m.addAttribute("grupos", grupos);
        m.addAttribute("conteos", conteos);
        m.addAttribute("totalServicios", lista.size());
        m.addAttribute("inactivos", inactivos);
        m.addAttribute("totalInactivos", inactivos.size());
        m.addAttribute("categoriaSeleccionada", cat != null ? cat.trim().toUpperCase() : "");
        m.addAttribute("codigoSugeridoInicial", servicios.generarCodigoSugerido(
                cat != null && !cat.isBlank() ? cat.trim().toUpperCase() : "CIRUGIA"));
        return "interno/catalogos/servicios-precios";
    }

    @PostMapping({"/interno/tipos-atencion/servicios/eliminar", "/interno/tipos_atencion/servicios/eliminar"})
    public String eliminarServicio(@RequestParam Long id, RedirectAttributes ra) {
        try {
            String mensaje = servicios.eliminarOSuspender(id);
            ra.addFlashAttribute("ok", mensaje);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", "No se pudo procesar: " + ex.getMessage());
        }
        return "redirect:/interno/tipos-atencion";
    }

    @PostMapping({"/interno/tipos-atencion/servicios/reactivar", "/interno/tipos_atencion/servicios/reactivar"})
    public String reactivarServicio(@RequestParam Long id, RedirectAttributes ra) {
        try {
            servicios.reactivar(id);
            ra.addFlashAttribute("ok", "Servicio reactivado e integrado nuevamente al tarifario.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", "No se pudo reactivar el servicio: " + ex.getMessage());
        }
        return "redirect:/interno/tipos-atencion";
    }

    @GetMapping("/interno/tipos-atencion/siguiente-codigo")
    @ResponseBody
    public Map<String, String> siguienteCodigo(@RequestParam String categoria) {
        String codigo = servicios.generarCodigoSugerido(categoria);
        return Map.of("codigo", codigo);
    }

    @PostMapping({"/interno/tipos-atencion/servicios", "/interno/tipos_atencion/servicios"})
    public String guardarServicio(@RequestParam(required = false) Long id,
                                  @RequestParam(required = false) String codigo,
                                  @RequestParam String nombre,
                                  @RequestParam String categoria,
                                  @RequestParam(required = false) String nuevaCategoria,
                                  @RequestParam(required = false) String subcategoria,
                                  @RequestParam(required = false) String descripcion,
                                  @RequestParam BigDecimal precio,
                                  @RequestParam Integer duracion,
                                  RedirectAttributes ra) {
        try {
            String catFinal = (nuevaCategoria != null && !nuevaCategoria.isBlank())
                    ? nuevaCategoria.trim().toUpperCase()
                    : categoria.trim().toUpperCase();
            String subcatFinal = (subcategoria == null || subcategoria.isBlank())
                    ? "General"
                    : subcategoria.trim();

            servicios.guardar(new ServicioDentalCatalogoService.ServicioInput(id, codigo, nombre,
                    catFinal, subcatFinal, descripcion, precio, duracion));
            ra.addFlashAttribute("ok", "Servicio guardado correctamente en la categoría " + catFinal + ".");
            return "redirect:/interno/tipos-atencion?cat=" + catFinal;
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", "No se pudo guardar el servicio: " + ex.getMessage());
            return "redirect:/interno/tipos-atencion";
        }
    }

    @PostMapping("/interno/tipos-atencion")
    public String guardarTipo(@RequestParam(required = false) Long idTipoAtencion,
                              @RequestParam String nombre,
                              @RequestParam(required = false) String descripcion,
                              @RequestParam int duracionMinutos,
                              RedirectAttributes ra) {
        try {
            if (nombre == null || nombre.trim().length() < 3) {
                throw new AgendaValidationException("El nombre del tipo de atención debe tener al menos 3 caracteres.");
            }
            if (duracionMinutos < 5 || duracionMinutos > 480) {
                throw new AgendaValidationException("La duración debe estar entre 5 y 480 minutos.");
            }
            service.guardarTipoAtencion(new TipoAtencion(idTipoAtencion, nombre.trim(), descripcion != null ? descripcion.trim() : null, duracionMinutos, true));
            ra.addFlashAttribute("ok", "Tipo de atención guardado correctamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/interno/tipos-atencion";
    }

    @PostMapping("/interno/tipos-atencion/{id}/estado")
    public String estadoTipo(@PathVariable long id, @RequestParam boolean activo, RedirectAttributes ra) {
        try {
            service.cambiarEstadoTipoAtencion(id, activo);
            ra.addFlashAttribute("ok", "Estado del tipo de atención actualizado.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/interno/tipos-atencion";
    }
}
