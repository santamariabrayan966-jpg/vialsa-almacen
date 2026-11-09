package com.dentia.service.interno;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="dentia.documento.provider", havingValue="miapi")
public class MiApiDocumentoExternoProvider implements DocumentoExternoProvider {

    private static final Logger log = LoggerFactory.getLogger(MiApiDocumentoExternoProvider.class);
    private static final String BASE_URL = "https://miapi.cloud/v1/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String token;

    @Autowired
    public MiApiDocumentoExternoProvider(
            RestTemplateBuilder builder,
            ObjectMapper objectMapper,
            @Value("${miapi.token}") String token) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.token = token;
    }

    MiApiDocumentoExternoProvider(RestTemplate restTemplate, ObjectMapper objectMapper, String token) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.token = token;
    }

    @Override
    public DocumentoExternoResultadoDto consultar(String documento) {
        if (token == null || token.isBlank()) {
            return DocumentoExternoResultadoDto.noDisponible(
                    "Configura MIAPI_TOKEN en el servidor para consultar documentos.");
        }
        String tipo = documento.length() == 8 ? "dni" : "ruc";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> respuesta = restTemplate.exchange(
                    BASE_URL + tipo + "/" + documento,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            JsonNode raiz = objectMapper.readTree(respuesta.getBody());
            JsonNode datos = raiz.has("data") ? raiz.get("data") : raiz.get("datos");
            if (datos == null || datos.isNull() || datos.isMissingNode()) {
                return DocumentoExternoResultadoDto.noEncontrado();
            }

            String direccion = direccionCompleta(datos.path("domiciliado"));
            if (direccion.isBlank()) direccion = valor(datos, "direccion");

            if (documento.length() == 8) {
                String nombres = valor(datos, "nombres");
                String apellidos = (valor(datos, "ape_paterno") + " " + valor(datos, "ape_materno")).trim();
                if ((nombres + apellidos).isBlank()) return DocumentoExternoResultadoDto.noEncontrado();
                return new DocumentoExternoResultadoDto(true, true, nombres, apellidos, "", direccion, fechaIso(valor(datos, "fecha_nacimiento")),
                        "Datos encontrados en la consulta externa.");
            }

            String razonSocial = valor(datos, "razon_social");
            if (razonSocial.isBlank()) razonSocial = valor(datos, "nombre_o_razon_social");
            if (razonSocial.isBlank()) return DocumentoExternoResultadoDto.noEncontrado();
            return new DocumentoExternoResultadoDto(true, true, "", "", razonSocial, direccion,
                    "Datos encontrados en la consulta externa.");
        } catch (HttpStatusCodeException ex) {
            int estado = ex.getStatusCode().value();
            if (estado == 404) return DocumentoExternoResultadoDto.noEncontrado();
            if (estado == 401 || estado == 403 || estado == 429 || estado >= 500) {
                log.warn("Consulta de documento no disponible. Estado HTTP {}", estado);
                return DocumentoExternoResultadoDto.noDisponible(
                        "La consulta externa no está disponible. Completa los datos manualmente.");
            }
            log.warn("La consulta externa rechazó la solicitud. Estado HTTP {}", estado);
            return DocumentoExternoResultadoDto.noDisponible(
                    "No fue posible consultar el documento. Completa los datos manualmente.");
        } catch (ResourceAccessException ex) {
            log.warn("Timeout o error de conexión consultando el documento externo");
            return DocumentoExternoResultadoDto.noDisponible(
                    "La consulta externa tardó demasiado. Completa los datos manualmente.");
        } catch (Exception ex) {
            log.error("Error procesando la consulta externa de documento", ex);
            return DocumentoExternoResultadoDto.noDisponible(
                    "No fue posible consultar el documento. Completa los datos manualmente.");
        }
    }

    private String fechaIso(String fecha) {
        if (fecha == null || fecha.isBlank()) return "";
        try {
            java.time.format.DateTimeFormatter formato = fecha.contains("/")
                    ? java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)
                    : java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
            return java.time.LocalDate.parse(fecha, formato).toString();
        } catch (java.time.format.DateTimeParseException ex) {
            log.warn("Formato de fecha de nacimiento no reconocido en MiAPI");
            return "";
        }
    }

    private String valor(JsonNode nodo, String campo) {
        return nodo != null && nodo.has(campo) && !nodo.get(campo).isNull()
                ? nodo.get(campo).asText("").trim()
                : "";
    }

    private String direccionCompleta(JsonNode domicilio) {
        if (domicilio == null || domicilio.isMissingNode() || domicilio.isNull()) return "";
        java.util.LinkedHashSet<String> partes = new java.util.LinkedHashSet<>();
        agregarParte(partes, valor(domicilio, "direccion"));
        agregarParte(partes, valor(domicilio, "distrito"));
        agregarParte(partes, valor(domicilio, "provincia"));
        agregarParte(partes, valor(domicilio, "departamento"));
        return String.join(", ", partes);
    }

    private void agregarParte(java.util.Set<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) partes.add(valor.trim());
    }
}
