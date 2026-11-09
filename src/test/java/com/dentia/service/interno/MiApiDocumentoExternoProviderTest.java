package com.dentia.service.interno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MiApiDocumentoExternoProviderTest {

    private RestTemplate restTemplate;
    private MiApiDocumentoExternoProvider provider;

    @BeforeEach
    void configurar() {
        restTemplate = mock(RestTemplate.class);
        provider = new MiApiDocumentoExternoProvider(restTemplate, new ObjectMapper(), "token-no-expuesto");
    }

    @Test
    void mapeaRespuestaDniValida() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"data":{"nombres":"Ana","ape_paterno":"Torres","ape_materno":"Díaz","fecha_nacimiento":"16/04/1994","domiciliado":{"direccion":"Calle Uno","distrito":"Lima"}}}
                        """));

        DocumentoExternoResultadoDto resultado = provider.consultar("12345678");

        assertTrue(resultado.encontrado());
        assertEquals("Ana", resultado.nombres());
        assertEquals("Torres Díaz", resultado.apellidos());
        assertEquals("1994-04-16", resultado.fechaNacimiento());
        assertEquals("Calle Uno, Lima", resultado.direccion());
    }

    @Test
    void estado429SeConvierteEnFallbackManual() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Sin saldo", null, null, null));

        DocumentoExternoResultadoDto resultado = provider.consultar("12345678");

        assertFalse(resultado.disponible());
        assertFalse(resultado.encontrado());
    }

    @Test
    void timeoutSeConvierteEnFallbackManual() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        DocumentoExternoResultadoDto resultado = provider.consultar("12345678");

        assertFalse(resultado.disponible());
        assertTrue(resultado.mensaje().contains("tardó"));
    }
}
