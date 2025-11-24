package com.vialsa.almacen.controller;

import com.vialsa.almacen.model.Cliente;
import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.ClienteService;
import com.vialsa.almacen.service.UsuarioServiceCliente;
import com.vialsa.almacen.service.ProductoService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistroController {

    private static final Logger log = LoggerFactory.getLogger(RegistroController.class);

    private final ClienteService clienteService;
    private final UsuarioServiceCliente usuarioServiceCliente;
    private final ProductoService productoService;

    public RegistroController(ClienteService clienteService,
                              UsuarioServiceCliente usuarioServiceCliente,
                              ProductoService productoService) {
        this.clienteService = clienteService;
        this.usuarioServiceCliente = usuarioServiceCliente;
        this.productoService = productoService;
    }

    @PostMapping("/registro-cliente")
    public String registrarCliente(
            @Valid @ModelAttribute("cliente") RegistroClienteDTO dto,
            BindingResult result,
            Model model) {

        log.info(">>> REGISTRO CLIENTE DTO: {}", dto.getCorreo());

        // ========== 1) VALIDACIONES DE DTO ==========
        if (result.hasErrors()) {
            model.addAttribute("productos", productoService.listarActivos());
            model.addAttribute("mostrarRegistro", true);
            return "tienda/tienda";
        }

        // ========== 2) Validar correo duplicado ==========
        if (usuarioServiceCliente.existeCorreo(dto.getCorreo())) {
            result.rejectValue("correo", "correo.exists", "El correo ya está registrado");
            model.addAttribute("productos", productoService.listarActivos());
            model.addAttribute("mostrarRegistro", true);
            return "tienda/tienda";
        }

        // ========== 3) Validar documento duplicado ==========
        if (clienteService.existeDocumento(dto.getNroDocumento())) {
            result.rejectValue("nroDocumento", "doc.exists", "El documento ya está registrado");
            model.addAttribute("productos", productoService.listarActivos());
            model.addAttribute("mostrarRegistro", true);
            return "tienda/tienda";
        }

        // ===========================================================
        // 4) Crear Usuario SOLO PARA CLIENTE → SE ASIGNA ROL CLIENTE
        // ===========================================================
        Usuario u = new Usuario();
        u.setCorreo(dto.getCorreo());
        u.setNombres(dto.getNombres());
        u.setApellidos(dto.getApellidos());

        // 👇 ESTE MÉTODO YA DEBE ASIGNAR idRol = 8 (CLIENTE) SIEMPRE
        Usuario usuarioCreado =
                usuarioServiceCliente.registrarNuevoClienteConPassword(u, dto.getPassword());

        // ===========================================================
        // 5) Crear Cliente (tabla clientes)
        // ===========================================================
        Cliente c = new Cliente();
        c.setNombres(dto.getNombres());
        c.setApellidos(dto.getApellidos());
        c.setCorreo(dto.getCorreo());
        c.setTelefono(dto.getTelefono());
        c.setNro_documento(dto.getNroDocumento());
        c.setDireccion(null);
        c.setIdTipoDocumento(dto.getIdTipoDocumento());
        c.setIdUsuario(usuarioCreado.getIdUsuario());

        clienteService.crear(c);

        log.info(">>> REGISTRO COMPLETADO: {}", dto.getCorreo());

        return "redirect:/tienda?registrado";
    }
}
