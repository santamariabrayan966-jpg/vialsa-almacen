package com.dentia.controller.interno;

import com.dentia.security.CustomAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/interno/modulos")
public class DentiaModuleController {

    private static final Map<String, ModuloPendiente> MODULOS = crearModulos();
    private final CustomAuthorizationService authorizationService;

    public DentiaModuleController(CustomAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{modulo}")
    public String modulo(@PathVariable String modulo, Authentication auth, Model model) {
        if ("citas".equals(modulo)) return "redirect:/interno/citas";
        if ("profesionales".equals(modulo)) return "redirect:/interno/profesionales";
        if ("pacientes".equals(modulo)) return "redirect:/interno/pacientes";
        if ("tratamientos".equals(modulo)) return "redirect:/interno/clinica/plan-tratamiento";
        if ("reportes".equals(modulo)) return "redirect:/interno/reportes";
        ModuloPendiente pendiente = MODULOS.get(modulo);
        if (pendiente == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!authorizationService.tieneAcceso(auth, pendiente.permiso())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (com.dentia.service.interno.GestionInternaService.MODULOS.contains(modulo)) {
            return "redirect:/interno/gestion/" + modulo;
        }
        if (com.dentia.service.interno.ClinicaInternaService.MODULOS_CLINICOS.contains(modulo)) {
            return "redirect:/interno/clinica/" + modulo;
        }
        model.addAttribute("tituloModulo", pendiente.titulo());
        model.addAttribute("descripcionModulo", pendiente.descripcion());
        return "interno/modulo-desarrollo";
    }

    private static Map<String, ModuloPendiente> crearModulos() {
        Map<String, ModuloPendiente> modulos = new LinkedHashMap<>();
        modulos.put("pacientes", new ModuloPendiente("Pacientes", "pacientes", "Gestion clinica y administrativa de pacientes."));
        modulos.put("citas", new ModuloPendiente("Agenda y citas", "citas", "Agenda real, disponibilidad y estados de atencion."));
        modulos.put("historia-clinica", new ModuloPendiente("Historia clinica", "historia_clinica", "Historia clinica versionada y protegida."));
        modulos.put("odontograma", new ModuloPendiente("Odontograma", "odontograma", "Registro odontologico por piezas y superficies."));
        modulos.put("diagnosticos", new ModuloPendiente("Diagnosticos", "diagnosticos", "Diagnosticos vinculados a la atencion."));
        modulos.put("tratamientos", new ModuloPendiente("Tratamientos", "tratamientos", "Catalogo y ejecucion de tratamientos."));
        modulos.put("plan-tratamiento", new ModuloPendiente("Plan de tratamiento", "plan_tratamiento", "Plan clinico propuesto al paciente."));
        modulos.put("presupuestos", new ModuloPendiente("Presupuestos", "presupuestos", "Presupuestos derivados del plan aprobado."));
        modulos.put("atenciones", new ModuloPendiente("Atenciones y evoluciones", "atenciones", "Atenciones, procedimientos y evolucion."));
        modulos.put("pagos", new ModuloPendiente("Pagos", "pagos", "Cobros, cuotas, saldos y comprobantes."));
        modulos.put("documentos", new ModuloPendiente("Documentos clinicos", "documentos", "Documentos y consentimientos con acceso protegido."));
        modulos.put("profesionales", new ModuloPendiente("Profesionales", "profesionales", "Perfiles profesionales vinculados a usuarios internos."));
        modulos.put("reportes", new ModuloPendiente("Reportes", "reportes", "Indicadores clinicos y administrativos."));
        return Map.copyOf(modulos);
    }

    private record ModuloPendiente(String titulo, String permiso, String descripcion) {}
}
