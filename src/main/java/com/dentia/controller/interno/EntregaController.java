package com.dentia.controller.interno;

import com.dentia.model.interno.Venta;
import com.dentia.service.interno.VentaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import com.dentia.service.interno.PermisoService;

@Controller
@RequestMapping("/interno/entregas")
public class EntregaController {

    private final VentaService ventaService;
    private final PermisoService permisoService;

    public EntregaController(VentaService ventaService,
                             PermisoService permisoService) {

        this.ventaService = ventaService;
        this.permisoService = permisoService;
    }

    private Integer obtenerSedeActiva(HttpSession session) {

        Object sedeObj = session.getAttribute("sedeActivaId");

        if (sedeObj instanceof Integer id) {
            return id;
        }

        if (sedeObj instanceof String sedeStr) {
            try {
                return Integer.parseInt(sedeStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    @GetMapping
    public String verEntregas(Model model,
                              Authentication auth,
                              HttpSession session,
                              @RequestParam(required = false, defaultValue = "TODOS") String filtroHistorial) {

        Integer idUsuario = ventaService.obtenerIdUsuarioPorNombre(auth.getName());

        Integer idSedeActiva = obtenerSedeActiva(session);

        if (idSedeActiva == null || idSedeActiva <= 0) {
            model.addAttribute("pendientes", List.of());
            model.addAttribute("pedidoActual", null);
            model.addAttribute("listos", List.of());
            model.addAttribute("pedidoBuscado", null);
            model.addAttribute("historial", List.of());
            model.addAttribute("filtroHistorial", filtroHistorial);

            model.addAttribute("totalHistorial", 0);
            model.addAttribute("totalHistorialAnulados", 0);
            model.addAttribute("totalHistorialInmediatas", 0);
            model.addAttribute("totalHistorialDespacho", 0);

            model.addAttribute("entregadosHoy", 0);
            model.addAttribute("preparandoTotal", 0);
            model.addAttribute("inmediatasHoy", 0);
            model.addAttribute("despachoHoy", 0);
            model.addAttribute("anuladosHoy", 0);

            model.addAttribute("errorSede", "Seleccione una sede activa para gestionar entregas.");

            return "interno/entregas/entregas";
        }

        model.addAttribute("pendientes", ventaService.listarPendientesEntregaPorSede(idSedeActiva));
        model.addAttribute("pedidoActual", ventaService.obtenerPedidoActualPorUsuarioYSede(idUsuario, idSedeActiva));
        model.addAttribute("listos", ventaService.listarPedidosListosPorSede(idSedeActiva));
        model.addAttribute("pedidoBuscado", null);

        List<Venta> historialFiltrado =
                ventaService.filtrarHistorialEntregasPorSede(filtroHistorial, idSedeActiva);

        model.addAttribute("historial", historialFiltrado);
        model.addAttribute("filtroHistorial", filtroHistorial);

        model.addAttribute("totalHistorial", historialFiltrado.size());
        model.addAttribute("totalHistorialAnulados", historialFiltrado.stream()
                .filter(h -> "ANULADO".equals(h.getNombreEstadoPedido())
                        || "ANULADA".equals(h.getEstadoVenta()))
                .count());

        model.addAttribute("totalHistorialInmediatas", historialFiltrado.stream()
                .filter(h -> !"ANULADO".equals(h.getNombreEstadoPedido())
                        && "INMEDIATA".equals(h.getTipoEntregaHistorial()))
                .count());

        model.addAttribute("totalHistorialDespacho", historialFiltrado.stream()
                .filter(h -> !"ANULADO".equals(h.getNombreEstadoPedido())
                        && "PROGRAMADA".equals(h.getTipoEntregaHistorial()))
                .count());
        model.addAttribute("entregadosHoy", ventaService.contarEntregadosHoyPorSede(idSedeActiva));
        model.addAttribute("preparandoTotal", ventaService.contarPreparandoPorSede(idSedeActiva));
        model.addAttribute("inmediatasHoy", ventaService.contarEntregasInmediatasHoyPorSede(idSedeActiva));
        model.addAttribute("despachoHoy", ventaService.contarEntregasDespachoHoyPorSede(idSedeActiva));
        model.addAttribute("anuladosHoy", ventaService.contarAnuladosHoyPorSede(idSedeActiva));

        return "interno/entregas/entregas";
    }

    @PostMapping("/aceptar/{idPedido}")
    public String aceptarPedido(@PathVariable Integer idPedido,
                                Authentication auth,
                                HttpSession session) {

        if (!permisoService.tienePermiso(auth, "entregas_preparacion")) {
            return "redirect:/interno/entregas";
        }

        Integer idUsuario = ventaService.obtenerIdUsuarioPorNombre(auth.getName());
        Integer idSedeActiva = obtenerSedeActiva(session);

        if (idSedeActiva == null || idSedeActiva <= 0) {
            return "redirect:/interno/entregas?error=Sede activa no seleccionada";
        }

        try {
            ventaService.aceptarPedidoPorSede(idPedido, idUsuario, idSedeActiva);
            return "redirect:/interno/entregas?aceptado";
        } catch (Exception e) {
            return "redirect:/interno/entregas?error=" + e.getMessage();
        }
    }
    @PostMapping("/listo/{idPedido}")
    public String marcarListo(@PathVariable Integer idPedido,
                              Authentication auth,
                              HttpSession session) {

        if (!permisoService.tienePermiso(auth, "entregas_preparacion")) {
            return "redirect:/interno/entregas";
        }

        Integer idSedeActiva = obtenerSedeActiva(session);

        if (idSedeActiva == null || idSedeActiva <= 0) {
            return "redirect:/interno/entregas?error=Sede activa no seleccionada";
        }

        try {
            ventaService.marcarPedidoListoPorSede(idPedido, idSedeActiva);
            return "redirect:/interno/entregas?listo";
        } catch (Exception e) {
            return "redirect:/interno/entregas?error=" + e.getMessage();
        }
    }

    @GetMapping("/buscar")
    public String buscarPedido(@RequestParam String codigo,
                               Model model,
                               Authentication auth,
                               HttpSession session) {

        if (!permisoService.tienePermiso(auth, "entregas_entrega")) {
            return "redirect:/interno/entregas";
        }

        Integer idUsuario = ventaService.obtenerIdUsuarioPorNombre(auth.getName());
        Integer idSedeActiva = obtenerSedeActiva(session);

        if (idSedeActiva == null || idSedeActiva <= 0) {
            return "redirect:/interno/entregas?error=Sede activa no seleccionada";
        }

        Venta pedidoBuscado =
                ventaService.buscarPedidoListoParaEntregaPorSede(codigo, idSedeActiva);

        List<Venta> historialFiltrado =
                ventaService.filtrarHistorialEntregasPorSede("TODOS", idSedeActiva);

        model.addAttribute("pendientes", ventaService.listarPendientesEntregaPorSede(idSedeActiva));
        model.addAttribute("pedidoActual", ventaService.obtenerPedidoActualPorUsuarioYSede(idUsuario, idSedeActiva));
        model.addAttribute("listos", ventaService.listarPedidosListosPorSede(idSedeActiva));
        model.addAttribute("pedidoBuscado", pedidoBuscado);
        model.addAttribute("codigoBuscado", codigo);

        model.addAttribute("historial", historialFiltrado);
        model.addAttribute("filtroHistorial", "TODOS");

        model.addAttribute("totalHistorial", historialFiltrado.size());
        model.addAttribute("totalHistorialAnulados", historialFiltrado.stream()
                .filter(h -> "ANULADO".equals(h.getNombreEstadoPedido())
                        || "ANULADA".equals(h.getEstadoVenta()))
                .count());

        model.addAttribute("totalHistorialInmediatas", historialFiltrado.stream()
                .filter(h -> !"ANULADO".equals(h.getNombreEstadoPedido())
                        && "INMEDIATA".equals(h.getTipoEntregaHistorial()))
                .count());

        model.addAttribute("totalHistorialDespacho", historialFiltrado.stream()
                .filter(h -> !"ANULADO".equals(h.getNombreEstadoPedido())
                        && "PROGRAMADA".equals(h.getTipoEntregaHistorial()))
                .count());

        model.addAttribute("entregadosHoy", ventaService.contarEntregadosHoyPorSede(idSedeActiva));
        model.addAttribute("preparandoTotal", ventaService.contarPreparandoPorSede(idSedeActiva));
        model.addAttribute("inmediatasHoy", ventaService.contarEntregasInmediatasHoyPorSede(idSedeActiva));
        model.addAttribute("despachoHoy", ventaService.contarEntregasDespachoHoyPorSede(idSedeActiva));
        model.addAttribute("anuladosHoy", ventaService.contarAnuladosHoyPorSede(idSedeActiva));

        return "interno/entregas/entregas";
    }

    @PostMapping("/entregar/{idPedido}")
    public String confirmarEntrega(@PathVariable Integer idPedido,
                                   @RequestParam String codigoRecojo,
                                   Authentication auth,
                                   HttpSession session) {

        if (!permisoService.tienePermiso(auth, "entregas_entrega")) {
            return "redirect:/interno/entregas";
        }

        Integer idUsuario = ventaService.obtenerIdUsuarioPorNombre(auth.getName());
        Integer idSedeActiva = obtenerSedeActiva(session);

        if (idSedeActiva == null || idSedeActiva <= 0) {
            return "redirect:/interno/entregas?error=Sede activa no seleccionada";
        }

        try {
            ventaService.confirmarEntregaPorSede(idPedido, idUsuario, codigoRecojo, idSedeActiva);
            return "redirect:/interno/entregas?entregado";
        } catch (Exception e) {
            return "redirect:/interno/entregas?error=" + e.getMessage();
        }
    }

    @GetMapping("/detalle/{idVenta}")
    @ResponseBody
    public Venta detalleEntrega(@PathVariable Integer idVenta,
                                HttpSession session) {

        Integer idSedeActiva = obtenerSedeActiva(session);

        if (idSedeActiva == null || idSedeActiva <= 0) {
            return null;
        }

        Venta venta = ventaService.buscarPorId(idVenta);

        if (venta == null) {
            return null;
        }

        if (venta.getIdSede() == null || !venta.getIdSede().equals(idSedeActiva)) {
            return null;
        }

        return venta;
    }


}
