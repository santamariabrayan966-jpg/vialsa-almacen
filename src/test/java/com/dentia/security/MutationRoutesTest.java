package com.dentia.security;

import com.dentia.controller.interno.CompraController;
import com.dentia.controller.interno.SedeController;
import com.dentia.controller.interno.VentaController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutationRoutesTest {
    @Test
    void accionesDeEstadoNoSeEjecutanPorGet() {
        for (Class<?> controller : List.of(CompraController.class, VentaController.class, SedeController.class)) {
            for (var method : controller.getDeclaredMethods()) {
                String name = method.getName().toLowerCase();
                if (!List.of("anular", "confirmar", "eliminar").stream().anyMatch(name::startsWith)) continue;
                assertFalse(method.isAnnotationPresent(GetMapping.class), controller.getSimpleName() + "." + name);
                assertTrue(method.isAnnotationPresent(PostMapping.class), controller.getSimpleName() + "." + name);
            }
        }
    }
}
