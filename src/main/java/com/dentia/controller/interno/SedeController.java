package com.dentia.controller.interno;

import com.dentia.model.interno.Sede;
import com.dentia.service.interno.SedeService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Map;

@Controller
@RequestMapping("/interno/sedes")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    @GetMapping
    public String listar(Model model,
                         @ModelAttribute(name = "success", binding = false) String success,
                         @ModelAttribute(name = "error", binding = false) String error) {

        var sedes = sedeService.listar();

        model.addAttribute("sedes", sedes);

        model.addAttribute("ciudades",
                sedes.stream()
                        .map(Sede::getCiudad)
                        .filter(c -> c != null && !c.isBlank())
                        .distinct()
                        .sorted()
                        .toList()
        );

        model.addAttribute("totalSedes", sedeService.totalSedes());
        model.addAttribute("totalAlmacenes", sedeService.totalAlmacenes());
        model.addAttribute("totalTiendas", sedeService.totalTiendas());
        model.addAttribute("totalInactivas", sedeService.totalInactivas());

        if (success != null && !success.isBlank()) {
            model.addAttribute("success", success);
        }

        if (error != null && !error.isBlank()) {
            model.addAttribute("error", error);
        }

        return "interno/sedes/sedes";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Sede> obtenerApi(@PathVariable Integer id) {
        Sede sede = sedeService.buscarPorId(id);

        if (sede == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sede);
    }

    @GetMapping("/horarios/{idSede}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerHorariosSede(@PathVariable Integer idSede) {

        Sede sede = sedeService.buscarPorId(idSede);

        if (sede == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sedeService.obtenerHorariosSede(idSede));
    }

    @PostMapping("/guardar")
    public Object guardar(@ModelAttribute("sede") Sede sede,
                          @RequestParam(value = "archivoFoto", required = false) MultipartFile archivoFoto,
                          @RequestParam(value = "eliminarFoto", defaultValue = "false") boolean eliminarFoto,
                          @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                          RedirectAttributes redirectAttributes) {

        boolean esAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {

            Sede existente = null;

            if (sede.getIdSede() != null) {
                existente = sedeService.buscarPorId(sede.getIdSede());
            }

            if (eliminarFoto) {

                if (existente != null && existente.getFoto() != null) {

                    Path rutaFoto = Paths.get(
                            System.getProperty("user.dir"),
                            "uploads",
                            "sedes",
                            existente.getFoto()
                    ).toAbsolutePath().normalize();

                    Files.deleteIfExists(rutaFoto.toAbsolutePath());

                    sede.setFoto(null);
                }

            } else if (archivoFoto != null && !archivoFoto.isEmpty()) {

                String contentType = archivoFoto.getContentType();

                if (contentType == null || !contentType.startsWith("image/")) {
                    return responderGuardar(esAjax, redirectAttributes, false, "Solo se permiten imágenes.");
                }

                if (existente != null && existente.getFoto() != null) {

                    Path anterior = Paths.get(
                            System.getProperty("user.dir"),
                            "uploads",
                            "sedes",
                            existente.getFoto()
                    ).toAbsolutePath().normalize();

                    Files.deleteIfExists(anterior.toAbsolutePath());
                }

                String nombreOriginal = archivoFoto.getOriginalFilename();

                String extension = com.dentia.service.shared.FileStorageService.extensionPermitida(archivoFoto);

                String nombreArchivo = "sede-" + UUID.randomUUID() + extension;

                Path rutaCarpeta = Paths.get(
                        System.getProperty("user.dir"),
                        "uploads",
                        "sedes"
                ).toAbsolutePath().normalize();

                Files.createDirectories(rutaCarpeta);

                Path rutaArchivo = rutaCarpeta.resolve(nombreArchivo)
                        .toAbsolutePath()
                        .normalize();

                Files.copy(
                        archivoFoto.getInputStream(),
                        rutaArchivo,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                sede.setFoto(nombreArchivo);

            } else if (existente != null) {

                sede.setFoto(existente.getFoto());
            }

            SedeService.ResultadoGuardarSede resultado = sedeService.guardar(sede);

            switch (resultado) {

                case NUEVO -> {
                    return responderGuardar(esAjax, redirectAttributes, true, "Sede o almacén guardado correctamente.");
                }

                case ACTUALIZADO -> {
                    return responderGuardar(esAjax, redirectAttributes, true, "Sede o almacén actualizado correctamente.");
                }

                case DUPLICADO -> {
                    return responderGuardar(esAjax, redirectAttributes, false, "Ya existe una sede o almacén con ese código.");
                }

                default -> {
                    return responderGuardar(esAjax, redirectAttributes, false, "Revisa los datos ingresados. El teléfono debe tener entre 6 y 20 caracteres y el link de Maps debe empezar por http:// o https://.");
                }
            }

        } catch (IOException e) {

            e.printStackTrace();
            return responderGuardar(esAjax, redirectAttributes, false, "No se pudo guardar la imagen.");
        }
    }

    private Object responderGuardar(boolean esAjax,
                                    RedirectAttributes redirectAttributes,
                                    boolean exito,
                                    String mensaje) {
        if (esAjax) {
            return ResponseEntity
                    .status(exito ? 200 : 400)
                    .body(Map.of("ok", exito, "message", mensaje));
        }

        redirectAttributes.addFlashAttribute(exito ? "success" : "error", mensaje);
        return "redirect:/interno/sedes";
    }

    @PostMapping("/horarios/guardar")
    public String guardarHorariosSede(@RequestParam("idSede") Integer idSede,
                                      @RequestParam(value = "descripcionPublica", required = false) String descripcionPublica,
                                      @RequestParam(value = "nombreBloque1", required = false) String nombreBloque1,
                                      @RequestParam(value = "horaInicio1", required = false) String horaInicio1,
                                      @RequestParam(value = "horaFin1", required = false) String horaFin1,
                                      @RequestParam(value = "nombreBloque2", required = false) String nombreBloque2,
                                      @RequestParam(value = "horaInicio2", required = false) String horaInicio2,
                                      @RequestParam(value = "horaFin2", required = false) String horaFin2,
                                      @RequestParam("duracionFranjaMinutos") Integer duracionFranjaMinutos,
                                      @RequestParam("capacidadPorFranja") Integer capacidadPorFranja,
                                      @RequestParam("activo") boolean activo,
                                      RedirectAttributes redirectAttributes) {

        try {

            sedeService.guardarHorariosSede(
                    idSede,
                    descripcionPublica,
                    nombreBloque1,
                    horaInicio1,
                    horaFin1,
                    nombreBloque2,
                    horaInicio2,
                    horaFin2,
                    duracionFranjaMinutos,
                    capacidadPorFranja,
                    activo
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Horarios de la sede actualizados correctamente."
            );

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/interno/sedes";
    }

    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Integer id,
                                @RequestParam("activo") boolean activo,
                                RedirectAttributes redirectAttributes) {

        boolean ok = sedeService.cambiarEstado(id, activo);

        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Estado actualizado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No se pudo actualizar el estado.");
        }

        return "redirect:/interno/sedes";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id,
                           RedirectAttributes redirectAttributes) {

        boolean ok = sedeService.eliminar(id);

        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Sede o almacén inactivado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "No se pudo inactivar la sede o almacén.");
        }

        return "redirect:/interno/sedes";
    }
    @GetMapping("/generar-codigo")
    @ResponseBody
    public ResponseEntity<String> generarCodigo(@RequestParam String tipo) {
        return ResponseEntity.ok(sedeService.generarCodigo(tipo));
    }
}
