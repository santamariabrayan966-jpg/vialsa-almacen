package com.dentia.controller.interno;

import com.dentia.dto.interno.ProveedorComprasResponseDto;
import com.dentia.dto.interno.ProveedorDetalleDto;
import com.dentia.dto.interno.ProveedorPaginaDto;
import com.dentia.model.interno.Proveedor;
import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.interno.ProveedorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/interno/proveedores")
public class ProveedorController {

    private static final Logger log = LoggerFactory.getLogger(ProveedorController.class);

    private final ProveedorService proveedorService;
    private final CustomAuthorizationService authorizationService;

    public ProveedorController(
            ProveedorService proveedorService,
            CustomAuthorizationService authorizationService) {
        this.proveedorService = proveedorService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public String listar(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(defaultValue = "") String search,
                         @ModelAttribute(name = "success", binding = false) String success,
                         @ModelAttribute(name = "error", binding = false) String error) {
        ProveedorPaginaDto pagina;
        try {
            pagina = proveedorService.obtenerPagina(page, size, search);
        } catch (Exception e) {
            log.error("No se pudo cargar el listado paginado de proveedores", e);
            int tamanioSeguro = List.of(5, 10, 20, 50).contains(size) ? size : 10;
            pagina = new ProveedorPaginaDto(List.of(), 0, tamanioSeguro, 0, 0, search.trim());
            model.addAttribute("error", "No se pudo cargar el listado de proveedores. Inténtalo nuevamente.");
        }
        model.addAttribute("titulo", "Proveedores | DentIA");
        model.addAttribute("proveedores", pagina.contenido());
        model.addAttribute("pagina", pagina);
        model.addAttribute("page", pagina.pagina());
        model.addAttribute("size", pagina.tamanio());
        model.addAttribute("search", pagina.busqueda());
        model.addAttribute("paginaInicio", Math.max(0, pagina.pagina() - 2));
        model.addAttribute("paginaFin", Math.min(pagina.totalPaginas() - 1, pagina.pagina() + 2));
        if (success != null && !success.isBlank()) model.addAttribute("success", success);
        if (error != null && !error.isBlank()) model.addAttribute("error", error);
        return "interno/proveedores/list";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerProveedorApi(
            @PathVariable Integer id, Authentication authentication) {
        boolean puedeVerCompras = tienePermisoCompras(authentication);
        ProveedorDetalleDto proveedor = proveedorService.obtenerDetalle(id, puedeVerCompras);
        if (proveedor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "El proveedor no existe o fue eliminado."));
        }
        return ResponseEntity.ok(proveedor);
    }

    @GetMapping("/api/{id}/compras")
    @ResponseBody
    public ResponseEntity<?> obtenerComprasProveedor(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(required = false) Integer idSede,
            @RequestParam(defaultValue = "") String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "8") int tamanio,
            Authentication authentication) {

        if (!tienePermisoCompras(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "No tienes permiso para consultar el historial de compras."));
        }

        try {
            ProveedorComprasResponseDto respuesta = proveedorService.obtenerHistorial(
                    id, busqueda, idSede, estado, fechaDesde, fechaHasta, pagina, tamanio);
            if (respuesta == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("mensaje", "El proveedor no existe o fue eliminado."));
            }
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al consultar compras del proveedor {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "No se pudo cargar el historial de compras."));
        }
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("titulo", "Nuevo Proveedor | DentIA");
        model.addAttribute("proveedor", new Proveedor());
        return "interno/proveedores/form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Proveedor proveedor = proveedorService.buscarPorId(id);
        if (proveedor == null || Integer.valueOf(2).equals(proveedor.getEstado())) {
            redirectAttributes.addFlashAttribute("error", "El proveedor no existe.");
            return "redirect:/interno/proveedores";
        }
        model.addAttribute("titulo", "Editar Proveedor | DentIA");
        model.addAttribute("proveedor", proveedor);
        return "interno/proveedores/form";
    }

    @PostMapping("/guardar")
    public Object guardar(
            @ModelAttribute("proveedor") Proveedor proveedor,
            @RequestHeader(value = "Accept", defaultValue = "") String accept,
            RedirectAttributes redirectAttributes) {

        ProveedorService.ResultadoOperacion resultado = proveedorService.guardar(proveedor);
        if (accept.contains("application/json")) {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("ok", resultado.exitoso());
            cuerpo.put("resultado", resultado.resultado().name());
            cuerpo.put("mensaje", resultado.mensaje());
            cuerpo.put("errores", resultado.errores());
            HttpStatus estado = resultado.exitoso() ? HttpStatus.OK : switch (resultado.resultado()) {
                case INVALIDO, DUPLICADO -> HttpStatus.BAD_REQUEST;
                case NO_ENCONTRADO, ELIMINADO -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            return ResponseEntity.status(estado).body(cuerpo);
        }

        redirectAttributes.addFlashAttribute(resultado.exitoso() ? "success" : "error", resultado.mensaje());
        return "redirect:/interno/proveedores";
    }

    @PostMapping("/eliminar/{id}")
    public Object eliminar(
            @PathVariable Integer id,
            @RequestHeader(value = "Accept", defaultValue = "") String accept,
            RedirectAttributes redirectAttributes) {
        boolean ok = proveedorService.eliminar(id);
        String mensaje = ok ? "Proveedor eliminado correctamente."
                : "No se pudo eliminar el proveedor.";
        if (accept.contains("application/json")) {
            return ResponseEntity.status(ok ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(Map.of("ok", ok, "mensaje", mensaje));
        }
        redirectAttributes.addFlashAttribute(ok ? "success" : "error", mensaje);
        return "redirect:/interno/proveedores";
    }

    @PostMapping("/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam("activo") boolean activo) {
        boolean ok = proveedorService.cambiarEstado(id, activo);
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "No se pudo actualizar el estado del proveedor."));
        }
        return ResponseEntity.ok(Map.of("ok", true, "activo", activo));
    }

    private boolean tienePermisoCompras(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authorizationService.tieneAcceso(authentication, "compras");
    }
}
