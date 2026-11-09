package com.dentia.controller.interno;

import com.dentia.model.interno.Compra;
import com.dentia.model.interno.CompraRecepcion;
import com.dentia.model.interno.CompraRecepcionDetalle;
import com.dentia.service.interno.CompraRecepcionService;
import com.dentia.service.interno.CompraService;
import com.dentia.service.interno.SedeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/interno/compras/recepcion")
public class CompraRecepcionController {

    private final CompraRecepcionService recepcionService;
    private final CompraService compraService;
    private final SedeService sedeService;

    public CompraRecepcionController(
            CompraRecepcionService recepcionService,
            CompraService compraService,
            SedeService sedeService
    ) {
        this.recepcionService = recepcionService;
        this.compraService = compraService;
        this.sedeService = sedeService;
    }

    @GetMapping("/{idCompra}")
    public String formularioRecepcion(
            @PathVariable Integer idCompra,
            Model model,
            HttpSession session
    ) {
        Compra compra = compraService.buscarPorId(idCompra);

        if (compra == null) {
            return "redirect:/interno/compras?error=CompraNoEncontrada";
        }

        List<CompraRecepcionDetalle> pendientes =
                recepcionService.listarPendientesPorCompra(idCompra);

        List<CompraRecepcion> recepciones =
                recepcionService.listarPorCompra(idCompra);

        BigDecimal valorFaltantePendiente =
                recepcionService.obtenerValorFaltantePendiente(idCompra);

        model.addAttribute("titulo", "Recepción de Compra | DentIA");
        model.addAttribute("compra", compra);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("recepciones", recepciones);
        model.addAttribute("valorFaltantePendiente", valorFaltantePendiente);
        model.addAttribute("fechaHoy", LocalDate.now());
        model.addAttribute("sedeActivaNombre", session.getAttribute("sedeActivaNombre"));
        model.addAttribute("sedes", sedeService.listar());

        return "interno/compras/recepcion-form";
    }

    @PostMapping("/guardar")
    public String guardarRecepcion(
            @RequestParam("idCompra") Integer idCompra,
            @RequestParam("nroGuiaRemision") String nroGuiaRemision,
            @RequestParam(value = "fechaRecepcion", required = false) String fechaRecepcion,
            @RequestParam(value = "observacion", required = false) String observacion,
            @RequestParam("idDetalleCompra") List<Integer> idDetalleCompra,
            @RequestParam("idProducto") List<Integer> idProducto,
            @RequestParam("idUnidad") List<Integer> idUnidad,
            @RequestParam("cantidadRecibida") List<String> cantidadRecibida,
            Model model,
            HttpSession session
    ) {
        try {
            Integer sedeActivaId = obtenerSedeActivaId(session);

            if (sedeActivaId == null) {
                throw new RuntimeException("No se encontró una sede activa para recepcionar la compra.");
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String nombreUsuario = auth != null ? auth.getName() : null;

            Integer idUsuario = compraService.obtenerIdUsuarioPorNombre(nombreUsuario);

            if (idUsuario == null) {
                throw new RuntimeException("No se encontró el usuario responsable.");
            }

            LocalDate fecha = (fechaRecepcion == null || fechaRecepcion.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(fechaRecepcion);

            recepcionService.registrarRecepcion(
                    idCompra,
                    nroGuiaRemision,
                    fecha,
                    sedeActivaId,
                    idUsuario,
                    observacion,
                    idDetalleCompra,
                    idProducto,
                    idUnidad,
                    cantidadRecibida
            );

            return "redirect:/interno/compras?recepcionRegistrada=true";

        } catch (Exception e) {
            Compra compra = compraService.buscarPorId(idCompra);

            List<CompraRecepcionDetalle> pendientes =
                    recepcionService.listarPendientesPorCompra(idCompra);

            BigDecimal valorFaltantePendiente =
                    recepcionService.obtenerValorFaltantePendiente(idCompra);

            model.addAttribute("titulo", "Recepción de Compra | DentIA");
            model.addAttribute("compra", compra);
            model.addAttribute("pendientes", pendientes);
            model.addAttribute("recepciones", recepcionService.listarPorCompra(idCompra));
            model.addAttribute("valorFaltantePendiente", valorFaltantePendiente);
            model.addAttribute("fechaHoy", LocalDate.now());
            model.addAttribute("errorRecepcion", e.getMessage());
            model.addAttribute("sedeActivaNombre", session.getAttribute("sedeActivaNombre"));
            model.addAttribute("sedes", sedeService.listar());

            return "interno/compras/recepcion-form";
        }
    }

    @PostMapping("/cerrar-faltante")
    public String cerrarFaltante(
            @RequestParam("idCompra") Integer idCompra,
            @RequestParam("motivo") String motivo,
            @RequestParam("tipoSustento") String tipoSustento,
            @RequestParam(value = "serieDocumento", required = false) String serieDocumento,
            @RequestParam(value = "numeroDocumento", required = false) String numeroDocumento,
            @RequestParam(value = "fechaDocumento", required = false) String fechaDocumento,
            @RequestParam(value = "montoAjustado", required = false) String montoAjustado,
            @RequestParam("estadoDevolucion") String estadoDevolucion,
            @RequestParam(value = "fechaDevolucion", required = false) String fechaDevolucion,
            @RequestParam(value = "observacion", required = false) String observacion,
            HttpSession session
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String nombreUsuario = auth != null ? auth.getName() : null;

            Integer idUsuario = compraService.obtenerIdUsuarioPorNombre(nombreUsuario);

            if (idUsuario == null) {
                throw new RuntimeException("No se encontró el usuario responsable.");
            }

            LocalDate fechaDoc = null;

            if (fechaDocumento != null && !fechaDocumento.isBlank()) {
                fechaDoc = LocalDate.parse(fechaDocumento);
            }

            LocalDate fechaDev = null;

            if (fechaDevolucion != null && !fechaDevolucion.isBlank()) {
                fechaDev = LocalDate.parse(fechaDevolucion);
            }

            BigDecimal monto = BigDecimal.ZERO;

            if (montoAjustado != null && !montoAjustado.isBlank()) {
                monto = new BigDecimal(montoAjustado.trim());
            }

            recepcionService.cerrarFaltante(
                    idCompra,
                    idUsuario,
                    motivo,
                    tipoSustento,
                    serieDocumento,
                    numeroDocumento,
                    fechaDoc,
                    monto,
                    estadoDevolucion,
                    fechaDev,
                    observacion
            );

            return "redirect:/interno/compras?faltanteCerrado=true";

        } catch (Exception e) {
            return "redirect:/interno/compras/recepcion/" + idCompra
                    + "?errorCerrarFaltante=" + e.getMessage();
        }
    }

    @PostMapping("/confirmar-devolucion")
    public String confirmarDevolucion(
            @RequestParam("idCompra") Integer idCompra,
            @RequestParam(value = "fechaDevolucion", required = false) String fechaDevolucion,
            @RequestParam(value = "observacionDevolucion", required = false) String observacionDevolucion
    ) {
        try {
            LocalDate fechaDev = null;

            if (fechaDevolucion != null && !fechaDevolucion.isBlank()) {
                fechaDev = LocalDate.parse(fechaDevolucion);
            }

            recepcionService.confirmarDevolucionProveedor(
                    idCompra,
                    fechaDev,
                    observacionDevolucion
            );

            return "redirect:/interno/compras?devolucionConfirmada=true";

        } catch (Exception e) {
            return "redirect:/interno/compras?errorDevolucion=" + e.getMessage();
        }
    }



    private Integer obtenerSedeActivaId(HttpSession session) {
        Object sedeObj = session.getAttribute("sedeActivaId");

        if (sedeObj instanceof Integer idSede) {
            return idSede;
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
}
