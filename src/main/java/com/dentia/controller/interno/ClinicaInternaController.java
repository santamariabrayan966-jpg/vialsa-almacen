package com.dentia.controller.interno;

import com.dentia.dto.clinica.ClinicaRequests.*;
import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.interno.ClinicaInternaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/interno/clinica")
public class ClinicaInternaController {

    private final ClinicaInternaService service;
    private final CustomAuthorizationService authService;

    public ClinicaInternaController(ClinicaInternaService service, CustomAuthorizationService authService) {
        this.service = service;
        this.authService = authService;
    }

    @ModelAttribute
    public void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private void autorizarModulo(String modulo, Authentication auth) {
        String permiso = modulo.replace("-", "_");
        if (modulo.equals("cuentas-pagos")) permiso = "pagos";
        if (modulo.equals("historial-citas")) permiso = "citas";
        if (!authService.tieneAcceso(auth, permiso)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a este módulo clínico.");
        }
    }

    private boolean permitido(String modulo, Authentication auth) {
        if ("plan-tratamiento".equals(modulo)) {
            return authService.tieneAcceso(auth, "plan_tratamiento") || authService.tieneAcceso(auth, "tratamientos");
        }
        String permiso = modulo.equals("cuentas-pagos") ? "pagos" : modulo.equals("historial-citas") ? "citas" : modulo.replace("-", "_");
        return authService.tieneAcceso(auth, permiso);
    }

    private void autorizarConsulta(Authentication auth) {
        if (ClinicaInternaService.MODULOS_CLINICOS.stream().noneMatch(m -> permitido(m, auth)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso al expediente clínico.");
    }

    @GetMapping({"", "/"})
    public String abrirExpediente(@RequestParam(required = false) Long pacienteId,
                                  @RequestParam(required = false) Long citaId,
                                  Authentication auth, RedirectAttributes redirect) {
        String modulo = ClinicaInternaService.MODULOS_CLINICOS.stream()
                .filter(m -> permitido(m, auth)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso al expediente clínico."));
        if (pacienteId != null) redirect.addAttribute("pacienteId", pacienteId);
        if (citaId != null) redirect.addAttribute("citaId", citaId);
        return "redirect:/interno/clinica/" + modulo;
    }

    @GetMapping("/tratamientos")
    public String redirigirTratamientos(@RequestParam(required = false) Long pacienteId,
                                        @RequestParam(required = false) Long citaId,
                                        Authentication auth,
                                        RedirectAttributes redirect) {
        if (!permitido("plan-tratamiento", auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso al módulo de tratamientos y planes.");
        }
        if (pacienteId != null) redirect.addAttribute("pacienteId", pacienteId);
        if (citaId != null) redirect.addAttribute("citaId", citaId);
        return "redirect:/interno/clinica/plan-tratamiento";
    }

    @GetMapping("/{modulo}")
    public String vistaModulo(@PathVariable String modulo,
                              @RequestParam(required = false) Long pacienteId,
                              @RequestParam(required = false) Long citaId,
                              Authentication auth,
                              Model model) {
        if (!ClinicaInternaService.MODULOS_CLINICOS.contains(modulo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo clínico no encontrado.");
        }
        autorizarModulo(modulo, auth);

        model.addAttribute("puedeOdontograma2d", authService.tieneAcceso(auth, "odontograma_2d"));
        model.addAttribute("puedeOdontograma3d", authService.tieneAcceso(auth, "odontograma_3d"));
        model.addAttribute("moduloClinica", modulo);
        model.addAttribute("pacienteInicialId", pacienteId != null ? pacienteId : "");
        model.addAttribute("citaInicialId", citaId != null ? citaId.toString() : "");
        model.addAttribute("modulosPermitidos", ClinicaInternaService.MODULOS_CLINICOS.stream().filter(m -> permitido(m, auth)).toList());
        model.addAttribute("profesionales", service.listarProfesionales());
        return "interno/clinica/index";
    }

    // --- APIS DE CONSULTA ---
    @GetMapping("/api/pacientes/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarPacientes(@RequestParam(required = false) String q, Authentication auth) {
        autorizarConsulta(auth);
        return service.buscarPacientes(q);
    }

    @GetMapping("/api/expediente/{pacienteId}")
    @ResponseBody
    public Map<String, Object> cargarExpediente(@PathVariable long pacienteId, Authentication auth) {
        autorizarConsulta(auth);
        var expediente = service.cargarExpedienteCompleto(pacienteId);
        if (!permitido("historia-clinica", auth)) expediente.remove("historia");
        if (!permitido("odontograma", auth)) {
            for (String campo : List.of("odontograma", "piezasOdontograma", "posicionesOdontograma", "historialOdontograma", "versionOdontograma")) expediente.remove(campo);
        }
        if (!authService.tieneAcceso(auth, "odontograma_3d")) expediente.remove("posicionesOdontograma");
        if (!permitido("diagnosticos", auth)) expediente.remove("diagnosticos");
        if (!permitido("plan-tratamiento", auth)) { expediente.remove("planes"); expediente.remove("ortodoncia"); }
        if (!permitido("atenciones", auth)) { expediente.remove("atenciones"); expediente.remove("borradorAtencion"); }
        if (!permitido("cuentas-pagos", auth)) expediente.remove("finanzas");
        if (!permitido("historial-citas", auth)) expediente.remove("citas");
        return expediente;
    }

    @GetMapping("/api/profesionales")
    @ResponseBody
    public List<Map<String, Object>> listarProfesionales(Authentication auth) {
        autorizarConsulta(auth);
        return service.listarProfesionales();
    }

    @GetMapping("/api/diagnosticos/catalogo")
    @ResponseBody
    public List<Map<String, Object>> catalogoDiagnosticos(@RequestParam(required = false) String categoria,
                                                          @RequestParam(required = false) String q,
                                                          Authentication auth) {
        autorizarModulo("diagnosticos", auth);
        return service.buscarCatalogoDiagnosticos(categoria, q);
    }

    @GetMapping("/api/tratamientos/catalogo")
    @ResponseBody
    public List<Map<String, Object>> catalogoTratamientos(@RequestParam(required = false) String categoria,
                                                          @RequestParam(required = false) String q,
                                                          Authentication auth) {
        autorizarConsulta(auth);
        return service.buscarCatalogoTratamientos(categoria, q);
    }

    @GetMapping("/api/planes")
    @ResponseBody
    public List<Map<String, Object>> listarPlanes(@RequestParam long pacienteId, Authentication auth) {
        autorizarModulo("plan-tratamiento", auth);
        return service.listarPlanes(pacienteId);
    }

    @GetMapping("/api/planes/{id}")
    @ResponseBody
    public Map<String, Object> detallePlan(@PathVariable long id, Authentication auth) {
        autorizarModulo("plan-tratamiento", auth);
        return service.detallePlan(id);
    }

    // --- ACCIONES Y MUTACIONES ---
    @PostMapping("/api/historia")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarHistoria(@RequestBody HistoriaInput input, Authentication auth) {
        autorizarModulo("historia-clinica", auth);
        service.guardarHistoria(input, auth.getName());
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Historia clínica guardada correctamente."));
    }

    @PostMapping("/api/odontograma/hallazgo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarHallazgo(@RequestBody OdontogramaPiezaInput input, Authentication auth) {
        autorizarModulo("odontograma", auth);
        service.registrarHallazgoOdontograma(input, auth.getName());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/odontograma/limpiar-pieza")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> limpiarPieza(@RequestBody LimpiarPiezaInput input, Authentication auth) {
        autorizarModulo("odontograma", auth);
        service.limpiarPiezaOdontograma(input, auth.getName());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/odontograma/nota")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> anotarPieza(@RequestBody NotaPiezaInput input, Authentication auth) {
        autorizarModulo("odontograma", auth);
        service.anotarPiezaOdontograma(input, auth.getName());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/odontograma/configuracion")
    @ResponseBody
    public Map<String, Object> configurarOdontograma(@RequestBody ConfiguracionOdontogramaInput input, Authentication auth) {
        autorizarModulo("odontograma", auth);
        if (!authService.tieneAcceso(auth, "odontograma_3d"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar el odontograma 3D.");
        service.configurarOdontograma(input, auth.getName());
        return Map.of("ok", true);
    }

    @PostMapping("/api/atenciones/borrador")
    @ResponseBody
    public Map<String, Object> guardarBorrador(@RequestBody BorradorAtencionInput input, Authentication auth) {
        autorizarModulo("atenciones", auth);
        return service.guardarBorrador(input, auth.getName());
    }

    @PostMapping("/api/diagnosticos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarDiagnostico(@RequestBody DiagnosticoInput input, Authentication auth) {
        autorizarModulo("diagnosticos", auth);
        long id = service.registrarDiagnostico(input, auth.getName());
        return ResponseEntity.ok(Map.of("ok", true, "id", id, "mensaje", "Diagnóstico registrado con éxito."));
    }

    @PostMapping("/api/diagnosticos/{id}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstadoDiagnostico(@PathVariable long id,
                                                                        @RequestBody EstadoInput input,
                                                                        Authentication auth) {
        autorizarModulo("diagnosticos", auth);
        service.actualizarEstadoDiagnostico(id, input.estado());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/tratamientos/catalogo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearTratamiento(@RequestBody TratamientoInput input, Authentication auth) {
        autorizarModulo("tratamientos", auth);
        long id = service.guardarTratamientoCatalogo(input);
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }

    @PostMapping("/api/planes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarPlan(@RequestParam(required = false) Long id,
                                                           @RequestBody PlanTratamientoInput input,
                                                           Authentication auth) {
        autorizarModulo("plan-tratamiento", auth);
        long planId = service.guardarPlanTratamiento(id, input, auth.getName());
        return ResponseEntity.ok(Map.of("ok", true, "id", planId, "mensaje", "Plan de tratamiento guardado."));
    }

    @PostMapping("/api/planes/{id}/estado")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstadoPlan(@PathVariable long id,
                                                                 @RequestBody EstadoInput input,
                                                                 Authentication auth) {
        autorizarModulo("plan-tratamiento", auth);
        service.cambiarEstadoPlan(id, input.estado());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/planes/{id}/generar-presupuesto")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generarPresupuesto(@PathVariable long id, Authentication auth) {
        autorizarModulo("plan-tratamiento", auth);
        long presupuestoId = service.generarPresupuestoDesdePlan(id, auth.getName());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "presupuestoId", presupuestoId,
                "mensaje", "Presupuesto #" + presupuestoId + " generado con éxito y enlazado con Gestión."
        ));
    }

    @PostMapping("/api/planes/{id}/aprobar-deuda")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> aprobarPlanYGenerarDeuda(@PathVariable long id, Authentication auth) {
        autorizarModulo("plan-tratamiento", auth);
        var res = service.aprobarPlanYGenerarDeuda(id, auth.getName());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/api/atenciones")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarAtencion(@RequestBody FinalizarAtencionInput input, Authentication auth) {
        autorizarModulo("atenciones", auth);
        long atencionId = service.registrarAtencion(input, auth.getName());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "id", atencionId,
                "mensaje", "Atención clínica (SOEP) registrada exitosamente."
        ));
    }
}
