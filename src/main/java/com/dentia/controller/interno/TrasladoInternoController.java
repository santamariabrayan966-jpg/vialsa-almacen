package com.dentia.controller.interno;

import com.dentia.service.interno.InventarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/interno/traslados-internos")
public class TrasladoInternoController {

    private final InventarioService inventarioService;

    public TrasladoInternoController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    private Integer obtenerUsuarioActualId(HttpSession session) {
        Object usuarioObj = session.getAttribute("usuarioActualId");

        if (usuarioObj instanceof Integer idUsuario) {
            return idUsuario;
        }

        if (usuarioObj instanceof String usuarioStr) {
            try {
                return Integer.parseInt(usuarioStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private String redirigirConError(String mensaje) {
        return redirigirConError(mensaje, "pendientes");
    }

    private String redirigirConError(String mensaje, String tab) {
        return "redirect:/interno/traslados-internos?tab="
                + URLEncoder.encode(tab, StandardCharsets.UTF_8)
                + "&error="
                + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private String guardarFotoTraslado(MultipartFile foto, String prefijo, Integer idTransferencia) {

        try {
            if (foto == null || foto.isEmpty()) {
                throw new RuntimeException("Debe subir una foto como evidencia.");
            }

            String contentType = foto.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("El archivo debe ser una imagen.");
            }

            String nombreOriginal = foto.getOriginalFilename() != null
                    ? foto.getOriginalFilename()
                    : "foto.jpg";

            String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(foto);

            String nombreArchivo = prefijo
                    + "_TRF_"
                    + idTransferencia
                    + "_"
                    + UUID.randomUUID()
                    + extension;

            Path carpeta = Paths.get(System.getProperty("user.dir"), "uploads", "traslados")
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(carpeta);

            Path destino = carpeta.resolve(nombreArchivo)
                    .toAbsolutePath()
                    .normalize();

            Files.copy(
                    foto.getInputStream(),
                    destino,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("========== FOTO TRASLADO ==========");
            System.out.println("USER DIR: " + System.getProperty("user.dir"));
            System.out.println("CARPETA: " + carpeta);
            System.out.println("DESTINO: " + destino);
            System.out.println("EXISTE ARCHIVO: " + Files.exists(destino));
            System.out.println("TAMAÑO ARCHIVO: " + Files.size(destino));
            System.out.println("===================================");

            return nombreArchivo;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("No se pudo guardar la foto del traslado. Detalle: " + e.getMessage());
        }
    }

    @GetMapping
    public String verTrasladosInternos(Model model, HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        boolean transportistaTieneTrasladoActivo =
                inventarioService.transportistaTieneTrasladoActivo(usuarioActualId);

        model.addAttribute("pendientes", inventarioService.listarTrasladosPendientes());
        model.addAttribute("enProceso", inventarioService.listarTrasladosEnProceso());
        model.addAttribute("porRecepcion", inventarioService.listarTrasladosPorRecepcion());
        model.addAttribute("historial", inventarioService.listarHistorialTraslados());

        model.addAttribute("transportistaTieneTrasladoActivo", transportistaTieneTrasladoActivo);

        return "interno/entregas/traslados-internos";
    }

    @GetMapping("/notificaciones-cambios")
    @ResponseBody
    public Map<String, Object> consultarNotificacionesCambios(@RequestParam String ids) {

        List<Integer> idsTransferencias = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();

        return inventarioService.buscarNotificacionCambioTraslado(idsTransferencias);
    }


    @PostMapping("/{idTransferencia}/aceptar")
    public String aceptarTraslado(@PathVariable Integer idTransferencia,
                                  HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            inventarioService.aceptarTrasladoInterno(idTransferencia, usuarioActualId);
            return "redirect:/interno/traslados-internos?tab=proceso&aceptado=true";
        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "pendientes");
        }
    }

    @PostMapping("/{idTransferencia}/llegada-origen")
    public String marcarLlegadaOrigen(@PathVariable Integer idTransferencia,
                                      @RequestParam("foto") MultipartFile foto,
                                      HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            String nombreFoto = guardarFotoTraslado(foto, "ORIGEN", idTransferencia);

            inventarioService.marcarLlegadaOrigenTraslado(
                    idTransferencia,
                    usuarioActualId,
                    nombreFoto
            );

            return "redirect:/interno/traslados-internos?tab=proceso&llegadaOrigen=true";

        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "proceso");
        }
    }

    @PostMapping("/{idTransferencia}/pedido-listo")
    public String marcarPedidoListo(@PathVariable Integer idTransferencia,
                                    @RequestParam("foto") MultipartFile foto,
                                    HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            String nombreFoto = guardarFotoTraslado(foto, "CARGA", idTransferencia);

            inventarioService.marcarPedidoListoTraslado(
                    idTransferencia,
                    usuarioActualId,
                    nombreFoto
            );

            return "redirect:/interno/traslados-internos?tab=proceso&pedidoListo=true";

        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "proceso");
        }
    }

    @PostMapping("/{idTransferencia}/iniciar-ruta")
    public String iniciarRuta(@PathVariable Integer idTransferencia,
                              HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            inventarioService.iniciarRutaTraslado(idTransferencia, usuarioActualId);
            return "redirect:/interno/traslados-internos?tab=proceso&ruta=true";
        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "proceso");
        }
    }

    @PostMapping("/{idTransferencia}/llegada-destino")
    public String marcarLlegadaDestino(@PathVariable Integer idTransferencia,
                                       @RequestParam("foto") MultipartFile foto,
                                       HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            String nombreFoto = guardarFotoTraslado(foto, "DESTINO", idTransferencia);

            inventarioService.marcarLlegadaDestinoTraslado(
                    idTransferencia,
                    usuarioActualId,
                    nombreFoto
            );

            return "redirect:/interno/traslados-internos?tab=recepcion&llegadaDestino=true";

        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "proceso");
        }
    }

    @PostMapping("/{idTransferencia}/reportar-incidencia")
    public String reportarIncidencia(@PathVariable Integer idTransferencia,
                                     @RequestParam String motivoIncidencia,
                                     @RequestParam String descripcionIncidencia,
                                     @RequestParam("fotoIncidencia") MultipartFile fotoIncidencia,
                                     HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            String nombreFoto = guardarFotoTraslado(
                    fotoIncidencia,
                    "IMPREVISTO",
                    idTransferencia
            );

            inventarioService.reportarIncidenciaTraslado(
                    idTransferencia,
                    usuarioActualId,
                    motivoIncidencia,
                    descripcionIncidencia,
                    nombreFoto
            );

            return "redirect:/interno/traslados-internos?tab=proceso&incidenciaReportada=true";

        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "proceso");
        }
    }

    @PostMapping("/{idTransferencia}/cancelar")
    public String cancelarTraslado(@PathVariable Integer idTransferencia,
                                   @RequestParam(required = false) String observacion,
                                   HttpSession session) {

        Integer usuarioActualId = obtenerUsuarioActualId(session);

        try {
            inventarioService.cancelarTrasladoInterno(idTransferencia, observacion, usuarioActualId);
            return "redirect:/interno/traslados-internos?tab=historial&cancelado=true";
        } catch (RuntimeException e) {
            return redirigirConError(e.getMessage(), "proceso");
        }
    }
}
