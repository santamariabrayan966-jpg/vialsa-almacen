package com.dentia.security;

import com.dentia.service.interno.PermisoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
@Component
public class GlobalPermissionsInjector {

    @Autowired
    private PermisoService permisoService;

    @ModelAttribute("permisosUsuario")
    public Map<String, Boolean> permisos(Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return Map.of(); // No logueado → sin permisos
        }

        try {
            return permisoService.getPermisos(auth);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
