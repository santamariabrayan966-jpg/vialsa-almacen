package com.dentia.controller.interno;

import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.UsuarioService;
import com.dentia.service.shared.FileStorageService;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilInternoController {

    private final UsuarioService usuarios;
    private final FileStorageService archivos;

    public PerfilInternoController(UsuarioService usuarios, FileStorageService archivos) {
        this.usuarios = usuarios;
        this.archivos = archivos;
    }

    @GetMapping("/interno/perfil")
    public String perfil(Model model) {
        Usuario u = usuarios.obtenerUsuarioActual();
        if (u == null) return "redirect:/interno/login";
        u.setContrasena(null);
        model.addAttribute("usuario", u);
        return "interno/usuarios/perfil-propio";
    }

    @PostMapping("/interno/perfil/editar")
    public String actualizarPerfil(@ModelAttribute Usuario datos,
                                   @RequestParam(required = false) MultipartFile fotoFile,
                                   RedirectAttributes redirect) {
        try {
            Usuario actual = usuarios.obtenerUsuarioActual();
            if (actual == null) throw new IllegalStateException("Sesión no encontrada.");

            actual.setNombres(datos.getNombres());
            actual.setApellidos(datos.getApellidos());
            actual.setTelefono(datos.getTelefono());
            actual.setCorreo(datos.getCorreo());
            actual.setNroDocumento(datos.getNroDocumento());
            actual.setDireccion(datos.getDireccion());

            if (fotoFile != null && !fotoFile.isEmpty()) {
                String nuevaFoto = archivos.guardarFotoUsuario(fotoFile);
                actual.setFoto(nuevaFoto);
            }

            usuarios.actualizarPerfil(actual);
            redirect.addFlashAttribute("ok", "Perfil actualizado correctamente.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "No se pudo actualizar el perfil.");
        }
        return "redirect:/interno/perfil";
    }

    @PostMapping("/interno/perfil/foto")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarFoto(@RequestParam MultipartFile fotoFile) {
        try {
            Usuario actual = usuarios.obtenerUsuarioActual();
            if (actual == null) return ResponseEntity.status(401).body(Map.of("error", "Sesión no encontrada."));
            String foto = archivos.guardarFotoUsuario(fotoFile);
            if (foto == null) return ResponseEntity.badRequest().body(Map.of("error", "Selecciona una imagen."));
            actual.setFoto(foto);
            usuarios.actualizarPerfil(actual);
            return ResponseEntity.ok(Map.of("url", "/uploads/usuarios/" + foto));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo guardar la foto."));
        }
    }

    @PostMapping("/interno/perfil/foto/eliminar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarFoto() {
        try {
            Usuario actual = usuarios.obtenerUsuarioActual();
            if (actual == null) return ResponseEntity.status(401).body(Map.of("error", "Sesión no encontrada."));
            actual.setFoto(null);
            usuarios.actualizarPerfil(actual);
            return ResponseEntity.ok(Map.of("mensaje", "Foto de perfil eliminada."));
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo eliminar la foto."));
        }
    }

    @PostMapping("/interno/perfil/contrasena")
    public String password(@RequestParam String contrasenaActual,
                           @RequestParam String nuevaContrasena, RedirectAttributes redirect) {
        try {
            Usuario u = usuarios.obtenerUsuarioActual();
            if (u == null) throw new IllegalStateException("No se identificó al usuario.");
            usuarios.cambiarContrasenaPropia(u.getIdUsuario(), contrasenaActual, nuevaContrasena);
            redirect.addFlashAttribute("ok", "Contraseña actualizada exitosamente.");
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/interno/perfil";
    }
}
