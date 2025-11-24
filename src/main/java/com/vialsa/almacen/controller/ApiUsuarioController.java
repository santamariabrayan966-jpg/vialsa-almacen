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
@RequestMapping("/api/usuario")
public class ApiUsuarioController {

    @Value("${miapi.token}")
    private String TOKEN;

    private static final String BASE_URL = "https://miapi.cloud/v1/";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/{dni}")
    @ResponseBody
    public ResponseEntity<?> consultarUsuario(@PathVariable String dni) {
        try {
            String url = BASE_URL + "dni/" + dni;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + TOKEN);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode json = mapper.readTree(response.getBody());

            JsonNode data = json.has("data") ? json.get("data") :
                    json.has("datos") ? json.get("datos") : null;

            if (data == null) {
                return ResponseEntity.status(404).body(Map.of("error", "No se encontraron datos"));
            }

            String nombres = get(data, "nombres");
            String apPat = get(data, "ape_paterno");
            String apMat = get(data, "ape_materno");
            String direccion = get(data.path("domiciliado"), "direccion");

            return ResponseEntity.ok(Map.of(
                    "nombres", nombres,
                    "apellidos", (apPat + " " + apMat).trim(),
                    "direccion", direccion
            ));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error consultando API"));
        }
    }

    private String get(JsonNode node, String key) {
        return node != null && node.has(key) && !node.get(key).isNull()
                ? node.get(key).asText()
                : "";
    }
}
