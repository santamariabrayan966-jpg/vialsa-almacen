package com.dentia.security;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.PermisoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomSuccessHandler.class);

    private final UsuarioDao usuarioDao;
    private final PermisoService permisoService;

    public CustomSuccessHandler(UsuarioDao usuarioDao,
                                PermisoService permisoService) {
        this.usuarioDao = usuarioDao;
        this.permisoService = permisoService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        String username = authentication.getName();

        boolean esPaciente = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_PACIENTE"));

        boolean esCliente = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_CLIENTE"));

        log.info("Inicio de sesion correcto; portal paciente: {}, cliente legado: {}", esPaciente, esCliente);

        if (esPaciente) {
            response.sendRedirect("/?googleSuccess#reservar-cita");
            return;
        }

        if (esCliente) {
            response.sendRedirect("/paciente");
            return;
        }

        usuarioDao.resetearIntentosFallidos(username);

        // =====================================================
        // SEDE ACTIVA INICIAL DEL USUARIO INTERNO
        // =====================================================
        Usuario usuario = usuarioDao.obtenerPorNombre(username)
                .orElseGet(() -> usuarioDao.findByCorreo(username).orElse(null));

        if (usuario == null) {
            log.warn("No se encontro usuario interno para cargar sede activa");
            response.sendRedirect("/acceso-denegado");
            return;
        }

        request.getSession().setAttribute("usuarioActualId", usuario.getIdUsuario());
        request.getSession().setAttribute("usuarioActualNombre", usuario.getNombreUsuario());
        request.getSession().setAttribute("usuarioActualRol", usuario.getNombreRol());

        request.getSession().setAttribute("sedeActivaId", usuario.getIdSedePrincipal());
        request.getSession().setAttribute("sedeActivaNombre", usuario.getNombreSedePrincipal());

        request.getSession().setAttribute("puedeCambiarSede", false);

        log.debug("Sede activa cargada para la sesion interna");

        Map<String, Boolean> permisos = permisoService.getPermisos(authentication);

        String destino = obtenerRutaInicial(permisos);

        log.debug("Redireccion posterior al inicio de sesion: {}", destino);

        response.sendRedirect(destino);
    }

    private String obtenerRutaInicial(Map<String, Boolean> permisos) {

        if (permisos == null || permisos.isEmpty()) {
            return "/acceso-denegado";
        }

        // =====================================================
        // ANÁLISIS
        // =====================================================
        if (tiene(permisos, "dashboard")) {
            return "/interno/dashboard";
        }


        // =====================================================
        // TRANSACCIONES
        // =====================================================
        if (tiene(permisos, "ventas")) {
            return "/interno/ventas";
        }

        if (tiene(permisos, "compras")) {
            return "/interno/compras";
        }

        // =====================================================
        // GESTIÓN
        // =====================================================
        if (tiene(permisos, "productos")) {
            return "/interno/productos";
        }

        if (tiene(permisos, "proveedores")) {
            return "/interno/proveedores";
        }

        if (tiene(permisos, "sedes")) {
            return "/interno/sedes";
        }

        if (tiene(permisos, "clientes")) {
            return "/interno/clientes";
        }

        // =====================================================
        // INVENTARIO
        // =====================================================
        if (tiene(permisos, "inventario_existencias")) {
            return "/interno/inventario/existencias";
        }

        if (tiene(permisos, "inventario_transferencias")) {
            return "/interno/inventario/transferencias";
        }

        if (tiene(permisos, "inventario_kardex")) {
            return "/interno/inventario/kardex";
        }

        if (tiene(permisos, "inventario_ajustes")) {
            return "/interno/inventario/ajustes";
        }

        // =====================================================
        // LOGÍSTICA
        // =====================================================
        if (tiene(permisos, "entregas_preparacion")
                || tiene(permisos, "entregas_entrega")
                || tiene(permisos, "entregas_historial")) {
            return "/interno/entregas";
        }

        if (tiene(permisos, "traslados_internos")) {
            return "/interno/traslados-internos";
        }

        // =====================================================
        // USUARIOS
        // =====================================================
        if (tiene(permisos, "usuarios")) {
            return "/interno/usuarios";
        }

        if (tiene(permisos, "roles")) {
            return "/interno/roles";
        }

        // =====================================================
        // TIENDA
        // =====================================================
        if (tiene(permisos, "tienda")) {
            return "/tienda";
        }

        return "/acceso-denegado";
    }

    private boolean tiene(Map<String, Boolean> permisos, String modulo) {
        return Boolean.TRUE.equals(permisos.get(modulo));
    }
}
