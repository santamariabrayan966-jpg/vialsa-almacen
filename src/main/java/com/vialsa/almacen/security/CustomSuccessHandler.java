package com.vialsa.almacen.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // =======================
        // LOGS PARA DEBUG
        // =======================
        System.out.println(">>> SUCCESS HANDLER EJECUTADO");
        System.out.println(">>> PRINCIPAL = " + authentication.getPrincipal().getClass().getSimpleName());
        System.out.println(">>> ROLES = " + authentication.getAuthorities());

        boolean esCliente = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_CLIENTE"));

        System.out.println(">>> ¿ES CLIENTE? = " + esCliente);

        // =======================
        // REDIRECCIÓN
        // =======================
        if (esCliente) {
            System.out.println(">>> REDIRIGIENDO A /tienda");
            response.sendRedirect("/tienda");
        } else {
            System.out.println(">>> REDIRIGIENDO A /dashboard");
            response.sendRedirect("/dashboard");
        }
    }
}
