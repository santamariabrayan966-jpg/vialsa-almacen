package com.vialsa.almacen.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/externo")
public class ApiDniController {

    @Value("${miapi.token}")
    private String TOKEN;

    private static final String BASE_URL = "https://miapi.cloud/v1/";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // =============================
    // CONSULTA DNI / RUC
    // =============================
    @GetMapping("/{tipo}/{numero}")
    @ResponseBody
    public ResponseEntity<?> consultarApi(
            @PathVariable("tipo") String tipo,
            @PathVariable("numero") String numero) {

        try {
            if (!tipo.equalsIgnoreCase("dni") && !tipo.equalsIgnoreCase("ruc")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Tipo inválido. Use 'dni' o 'ruc'."));
            }

            // Construcción del endpoint API externo
            String url = BASE_URL + tipo + "/" + numero;

            System.out.println("🌍 Consultando API externa: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + TOKEN);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            System.out.println("📥 Respuesta de la API: " + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(response.getStatusCode())
                        .body(Map.of("error", "Error HTTP desde miapi: " + response.getStatusCode()));
            }

            JsonNode json = mapper.readTree(response.getBody());

            // miapi a veces devuelve "data" o "datos"
            JsonNode data = json.has("data") ? json.get("data") :
                    json.has("datos") ? json.get("datos") : null;

            if (data == null || data.isMissingNode()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No se encontraron datos del documento."));
            }

            // =============================
            //   MAPEAR RESPUESTA API
            // =============================
            String nombres = "";
            String apellidos = "";
            String direccion = "";

            if (tipo.equalsIgnoreCase("dni")) {
                nombres = safeGet(data, "nombres");
                apellidos = (safeGet(data, "ape_paterno") + " " +
                        safeGet(data, "ape_materno")).trim();

                direccion = safeGet(data.path("domiciliado"), "direccion");

            } else { // RUC
                nombres = safeGet(data, "razon_social");
                if (nombres.isBlank()) {
                    nombres = safeGet(data, "nombre_o_razon_social");
                }

                apellidos = ""; // empresas no tienen apellidos

                direccion = safeGet(data.path("domiciliado"), "direccion");
                if (direccion.isBlank()) {
                    direccion = safeGet(data, "direccion");
                }
            }

            // =============================
            // DEVOLVER SOLO DATOS (NO GUARDAR)
            // =============================
            return ResponseEntity.ok(Map.of(
                    "data", Map.of(
                            "nombre_completo", (nombres + " " + apellidos).trim(),
                            "nombres", nombres,
                            "apellidos", apellidos,
                            "direccion", direccion
                    )
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar API: " + e.getMessage()));
        }
    }

    private String safeGet(JsonNode node, String key) {
        return (node != null && node.has(key) && !node.get(key).isNull())
                ? node.get(key).asText()
                : "";
    }
}
