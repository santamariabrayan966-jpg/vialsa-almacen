package com.dentia.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Alias heredado. La consulta real reutiliza primero los datos locales. */
@RestController
@RequestMapping("/api/usuario")
public class ApiUsuarioController {
    private final ApiDniController documentos;

    public ApiUsuarioController(ApiDniController documentos) {
        this.documentos = documentos;
    }

    @GetMapping("/{dni}")
    public ResponseEntity<?> consultarUsuario(@PathVariable String dni) {
        if (dni == null || !dni.matches("\\d{8}")) {
            return ResponseEntity.badRequest().body(Map.of("error", "DNI invalido"));
        }
        ResponseEntity<?> respuesta = documentos.consultarApi("dni", dni);
        if (!respuesta.getStatusCode().is2xxSuccessful()) return respuesta;
        if (!(respuesta.getBody() instanceof Map<?, ?> body) || !(body.get("data") instanceof Map<?, ?> data)) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Consulta no disponible"));
        }
        return ResponseEntity.ok(Map.of(
                "nombres", texto(data.get("nombres")),
                "apellidos", texto(data.get("apellidos")),
                "direccion", texto(data.get("direccion"))));
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
