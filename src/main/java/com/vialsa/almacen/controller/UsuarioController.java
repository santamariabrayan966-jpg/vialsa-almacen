package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.FileStorageService;
import com.vialsa.almacen.service.RolService;
import com.vialsa.almacen.service.UsuarioService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final RolService rolService;
    private final FileStorageService fileStorageService;

    public UsuarioController(
            UsuarioService usuarioService,
            RolService rolService,
            FileStorageService fileStorageService
    ) {
        this.usuarioService = usuarioService;
        this.rolService = rolService;
        this.fileStorageService = fileStorageService;
    }

    // =============================================================
    // LISTADO
    // =============================================================
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarUsuariosInternos());
        model.addAttribute("roles", rolService.listarRoles());
        model.addAttribute("usuario", new Usuario());
        return "usuarios/list";
    }

    // =============================================================
// GUARDAR NUEVO USUARIO
// =============================================================
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult result,
            @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile,
            Model model
    ) {

        System.out.println(">>> ROL RECIBIDO DEL FORMULARIO = " + usuario.getIdRol());
        System.out.println(">>> USUARIO RECIBIDO = " + usuario.getNombreUsuario());
        System.out.println(">>> NOMBRES = " + usuario.getNombres());
        System.out.println(">>> APELLIDOS = " + usuario.getApellidos());
        // Normalizar: si viene vacío, poner null
        if (usuario.getIdRol() != null && usuario.getIdRol() == 0) {
            usuario.setIdRol(null);
        }


        // ❌ VALIDACIÓN: ROL NO SELECCIONADO
        if (usuario.getIdRol() == null) {
            result.rejectValue("idRol", "rol.vacio", "Debes seleccionar un rol.");
        }


        // ❌ VALIDACIÓN: ROL CLIENTE PROHIBIDO
        if (usuario.getIdRol() != null && usuario.getIdRol() == 8) {
            result.rejectValue("idRol", "rol.invalido",
                    "No puedes asignar el rol CLIENTE desde este módulo.");
        }

        // Validación imagen
        if (fotoFile != null && !fotoFile.isEmpty()) {
            if (!fotoFile.getContentType().startsWith("image/")) {
                result.rejectValue("foto", "foto.invalida", "La foto debe ser una imagen válida");
            }
        }

        // Si hay errores → mantener modal abierto
        if (result.hasErrors()) {
            model.addAttribute("usuarios", usuarioService.listarUsuariosInternos());
            model.addAttribute("roles", rolService.listarRoles());
            model.addAttribute("modalNuevo", true);
            return "usuarios/list";
        }

        // Guardar foto
        if (fotoFile != null && !fotoFile.isEmpty()) {
            usuario.setFoto(fileStorageService.guardarFotoUsuario(fotoFile));
        }

        if (usuario.getActivo() == null) usuario.setActivo(true);

        if (usuario.getIdTipoDocumento() == null) {
            usuario.setIdTipoDocumento(1); // 1 = DNI
        }


        usuarioService.guardarUsuarioInterno(usuario);

        return "redirect:/usuarios?guardado";
    }
    // =============================================================
//  NUEVO: GUARDAR USUARIO (AJAX) SIN RECARGAR PÁGINA
// =============================================================
    @PostMapping("/guardar-ajax")
    @ResponseBody
    public Map<String, Object> guardarAjax(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult result,
            @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile
    ) {

        Map<String, Object> resp = new HashMap<>();

        // 🔴 Si hay errores de validación, los devolvemos sin cerrar modal
        if (result.hasErrors()) {
            resp.put("status", "error");
            resp.put("errors", result.getAllErrors());
            return resp;
        }

        try {

            // Guardar foto si existe
            if (fotoFile != null && !fotoFile.isEmpty()) {
                usuario.setFoto(fileStorageService.guardarFotoUsuario(fotoFile));
            }

            // Documento por defecto
            if (usuario.getIdTipoDocumento() == null) {
                usuario.setIdTipoDocumento(1);
            }

            usuarioService.guardarUsuarioInterno(usuario);

            resp.put("status", "ok");
            return resp;

        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("errors", List.of(
                    Map.of("field", "general", "defaultMessage", e.getMessage())
            ));
            return resp;
        }
    }


    // =============================================================
    // JSON PARA MODAL EDITAR
    // =============================================================
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Usuario> obtenerUsuarioJson(@PathVariable Integer id) {
        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        // No exponemos la contraseña al front
        usuario.setContrasena(null);
        return ResponseEntity.ok(usuario);
    }


    // =============================================================
    // ACTUALIZAR USUARIO
    // =============================================================
    @PostMapping("/actualizar")
    public String actualizar(
            @Valid @ModelAttribute Usuario usuario,
            BindingResult result,
            @RequestParam(value = "fotoFile", required = false) MultipartFile fotoFile,
            @RequestParam(value = "nuevaContrasena", required = false) String nuevaContrasena,
            Model model
    ) {

        Usuario original = usuarioService.buscarPorId(usuario.getIdUsuario());
        if (original == null) {
            return "redirect:/usuarios?noEncontrado";
        }

        // Validación foto
        if (fotoFile != null && !fotoFile.isEmpty()) {
            if (fotoFile.getContentType() == null || !fotoFile.getContentType().startsWith("image/")) {
                result.rejectValue("foto", "foto.invalida", "La foto debe ser una imagen válida");
            }
        }

        // Validación contraseña fuerte (si el usuario intenta cambiarla)
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            if (!nuevaContrasena.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$")) {
                result.rejectValue(
                        "contrasena",
                        "contrasena.invalida",
                        "La contraseña debe tener mayúscula, minúscula, número y símbolo"
                );
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("usuarios", usuarioService.listarUsuariosInternos());
            model.addAttribute("roles", rolService.listarRoles());
            model.addAttribute("modalEditar", usuario.getIdUsuario());
            return "usuarios/list";
        }

        // Foto final: si no se envía una nueva, se deja la que ya tenía
        String fotoFinal = original.getFoto();
        if (fotoFile != null && !fotoFile.isEmpty()) {
            fotoFinal = fileStorageService.guardarFotoUsuario(fotoFile);
        }
        usuario.setFoto(fotoFinal);

        // 🔧 Normalizar campos vacíos a null
        if (usuario.getCorreo() != null && usuario.getCorreo().isBlank()) {
            usuario.setCorreo(null);
        }
        if (usuario.getTelefono() != null && usuario.getTelefono().isBlank()) {
            usuario.setTelefono(null);
        }
        if (usuario.getNroDocumento() != null && usuario.getNroDocumento().isBlank()) {
            usuario.setNroDocumento(null);
        }
        if (usuario.getNombres() != null && usuario.getNombres().isBlank()) {
            usuario.setNombres(null);
        }
        if (usuario.getApellidos() != null && usuario.getApellidos().isBlank()) {
            usuario.setApellidos(null);
        }

        // ACTUALIZAR (el service decide si usa la nueva contraseña o mantiene la anterior)
        usuarioService.actualizarUsuarioInterno(usuario, nuevaContrasena, original.getContrasena());

        return "redirect:/usuarios?actualizado";
    }

    // =============================================================
    // ELIMINAR
    // =============================================================
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return "redirect:/usuarios?eliminado";
    }
    // =============================================================
// ACTIVAR USUARIO
// =============================================================
    @GetMapping("/activar/{id}")
    public String activar(@PathVariable Integer id) {
        usuarioService.cambiarEstadoActivo(id, true);
        return "redirect:/usuarios?activado";
    }

    // =============================================================
// DESACTIVAR USUARIO
// =============================================================
    @GetMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Integer id) {
        usuarioService.cambiarEstadoActivo(id, false);
        return "redirect:/usuarios?desactivado";
    }

}
