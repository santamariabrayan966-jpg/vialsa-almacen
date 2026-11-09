package com.dentia.controller.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.model.interno.MarcaIdentidadConfig;
import com.dentia.service.interno.MarcaIdentidadService;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PwaControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manifiestoUsaNombreColoresEIconosDeLaConfiguracion() throws Exception {
        MarcaIdentidadConfig config = new MarcaIdentidadConfig();
        config.setNombreComercial("Clínica Sonrisa");
        config.setDescripcionCorta("Odontología integral");
        config.setColorPrimario("#123456");
        config.setFechaActualizacion(LocalDateTime.of(2026, 9, 22, 12, 0));
        MarcaIdentidadService service = mock(MarcaIdentidadService.class);
        when(service.obtenerConfiguracion()).thenReturn(config);

        var response = new PwaController(service, objectMapper).manifest();
        JsonNode manifest = objectMapper.readTree(response.getBody());

        assertEquals("Clínica Sonrisa · Gestión odontológica", manifest.get("name").asText());
        assertEquals("#123456", manifest.get("theme_color").asText());
        assertEquals("/interno/login?source=pwa", manifest.get("start_url").asText());
        assertEquals(3, manifest.get("icons").size());
        assertEquals("512x512", manifest.get("icons").get(1).get("sizes").asText());
    }

    @Test
    void iconosSiempreTienenLasDimensionesDeclaradasAunqueNoHayaLogo() throws Exception {
        MarcaIdentidadService service = mock(MarcaIdentidadService.class);
        when(service.obtenerConfiguracion()).thenReturn(new MarcaIdentidadConfig());
        PwaController controller = new PwaController(service, objectMapper);

        var response = controller.icono(192);
        var image = ImageIO.read(new ByteArrayInputStream(response.getBody()));

        assertNotNull(image);
        assertEquals(192, image.getWidth());
        assertEquals(192, image.getHeight());
        assertTrue(controller.icono(144).getStatusCode().is4xxClientError());
    }

    @Test
    void serviceWorkerNoGuardaPaginasClinicasEnCache() {
        MarcaIdentidadService service = mock(MarcaIdentidadService.class);
        String worker = new PwaController(service, objectMapper).serviceWorker().getBody();

        assertNotNull(worker);
        assertTrue(worker.contains("skipWaiting"));
        assertFalse(worker.contains("caches.open"));
        assertFalse(worker.contains("fetch("));
    }
}
