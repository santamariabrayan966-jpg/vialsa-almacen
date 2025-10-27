package com.vialsa.almacen.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vialsa.almacen.dao.interfaces.IClienteDao;
import com.vialsa.almacen.model.Cliente;
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
    private final IClienteDao clienteDao;

    public ApiDniController(IClienteDao clienteDao) {
        this.clienteDao = clienteDao;
    }

    @GetMapping("/{tipo}/{numero}")
    @ResponseBody
    public ResponseEntity<?> consultarApi(@PathVariable("tipo") String tipo,
                                          @PathVariable("numero") String numero) {
        try {
            if (!tipo.equalsIgnoreCase("dni") && !tipo.equalsIgnoreCase("ruc")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tipo inválido. Use 'dni' o 'ruc'."));
            }

            // Verificar si el cliente ya existe en la base
            Cliente existente = clienteDao.buscarPorDocumento(numero);
            if (existente != null) {
                System.out.println("✅ Cliente ya existe: " + existente.getNombres());
                return ResponseEntity.ok(Map.of(
                        "data", Map.of(
                                "idCliente", existente.getIdClientes(),
                                "nombre_completo", existente.getNombres() + " " + existente.getApellidos()
                        )
                ));
            }

            // Consultar la API externa
            String url = BASE_URL + tipo + "/" + numero;
            System.out.println("🌍 Consultando API externa: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + TOKEN);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("📥 Respuesta de la API: " + response.getBody());

            JsonNode json = mapper.readTree(response.getBody());
            JsonNode data = json.has("data") ? json.get("data") :
                    json.has("datos") ? json.get("datos") : json;

            // Crear el cliente según el tipo de documento
            Cliente nuevo = new Cliente();
            nuevo.setNro_documento(numero);

            if (tipo.equalsIgnoreCase("dni")) {
                String nombres = safeGet(data, "nombres");
                String apPaterno = safeGet(data, "ape_paterno");
                String apMaterno = safeGet(data, "ape_materno");

                nuevo.setNombres(nombres);
                nuevo.setApellidos(apPaterno + " " + apMaterno);
                nuevo.setDireccion(safeGet(data.path("domiciliado"), "direccion"));
            } else { // Caso RUC
                nuevo.setNombres(safeGet(data, "nombre_o_razon_social"));
                nuevo.setApellidos("");
                nuevo.setDireccion(safeGet(data, "direccion"));
            }

            // Guardar el nuevo cliente
            clienteDao.registrar(nuevo);

            // Buscarlo nuevamente para obtener su ID
            Cliente guardado = clienteDao.buscarPorDocumento(numero);
            if (guardado == null) {
                throw new IllegalStateException("No se pudo registrar el cliente en la base de datos.");
            }

            return ResponseEntity.ok(Map.of(
                    "data", Map.of(
                            "idCliente", guardado.getIdClientes(),
                            "nombre_completo", guardado.getNombres() + " " + guardado.getApellidos()
                    )
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar o registrar el cliente: " + e.getMessage()));
        }
    }

    private String safeGet(JsonNode node, String key) {
        return (node != null && node.has(key) && !node.get(key).isNull()) ? node.get(key).asText() : "";
    }
}
