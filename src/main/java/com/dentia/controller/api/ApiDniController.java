package com.dentia.controller.api;

import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;
import com.dentia.service.interno.DocumentoExternoProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/api/externo")
public class ApiDniController {

    private final DocumentoExternoProvider documentoExternoProvider;
    private final JdbcTemplate jdbc;

    public ApiDniController(DocumentoExternoProvider documentoExternoProvider, JdbcTemplate jdbc) {
        this.documentoExternoProvider = documentoExternoProvider;
        this.jdbc = jdbc;
    }

    @GetMapping("/{tipo}/{numero}")
    @ResponseBody
    public ResponseEntity<?> consultarApi(
            @PathVariable String tipo,
            @PathVariable String numero) {
        boolean dni = "dni".equalsIgnoreCase(tipo) && numero.matches("\\d{8}");
        boolean ruc = "ruc".equalsIgnoreCase(tipo) && numero.matches("\\d{11}");
        if (!dni && !ruc) {
            return ResponseEntity.badRequest().body(Map.of("error", "Documento inválido."));
        }

        Map<String, Object> almacenado = buscarEnBase(numero);
        if (almacenado != null && (!dni || (almacenado.get("fecha_nacimiento") != null
                && almacenado.get("direccion") != null && !almacenado.get("direccion").toString().isBlank()))) {
            return ResponseEntity.ok(Map.of("data", almacenado, "origen", "base_datos"));
        }

        DocumentoExternoResultadoDto resultado = documentoExternoProvider.consultar(numero);
        if (almacenado != null && (!resultado.disponible() || !resultado.encontrado())) {
            return ResponseEntity.ok(Map.of("data", almacenado, "origen", "base_datos"));
        }
        if (!resultado.disponible()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El servicio de consulta no está disponible."));
        }
        if (!resultado.encontrado()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se encontraron datos del documento."));
        }

        String nombreCompleto = ruc
                ? resultado.razonSocial()
                : (resultado.nombres() + " " + resultado.apellidos()).trim();
        return ResponseEntity.ok(Map.of("data", Map.of(
                "nombre_completo", nombreCompleto,
                "nombres", resultado.nombres(),
                "apellidos", resultado.apellidos(),
                "razon_social", resultado.razonSocial(),
                "direccion", resultado.direccion(),
                "fecha_nacimiento", resultado.fechaNacimiento()
        ), "origen", "externo"));
    }

    /**
     * Reutiliza datos ya guardados para evitar consumir consultas externas.
     * Se consulta por DNI en las tres fichas administrativas que pueden tenerlo.
     */
    private Map<String, Object> buscarEnBase(String dni) {
        String[] consultas = {
                "SELECT nombres, apellidos, direccion, fecha_nacimiento FROM pacientes WHERE dni = ? LIMIT 1",
                "SELECT Nombres AS nombres, Apellidos AS apellidos, direccion, NULL AS fecha_nacimiento FROM usuarios WHERE NroDocumento = ? LIMIT 1",
                "SELECT nombres, apellidos, NULL AS direccion, NULL AS fecha_nacimiento FROM profesionales WHERE documento = ? LIMIT 1"
        };
        for (String sql : consultas) {
            try {
                var filas = jdbc.queryForList(sql, dni);
                if (!filas.isEmpty()) {
                    Map<String, Object> fila = new LinkedHashMap<>(filas.get(0));
                    fila.putIfAbsent("nombre_completo", ((fila.get("nombres") == null ? "" : fila.get("nombres")) + " "
                            + (fila.get("apellidos") == null ? "" : fila.get("apellidos"))).trim());
                    return fila;
                }
            } catch (RuntimeException ignored) {
                // Una tabla heredada puede no tener aún la columna opcional.
            }
        }
        return null;
    }
}
