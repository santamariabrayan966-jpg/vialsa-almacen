package com.dentia.service.interno;

import com.dentia.dto.interno.ventas.ClienteDocumentoConsultaDto;
import com.dentia.dto.interno.ventas.DocumentoExternoResultadoDto;
import com.dentia.model.interno.Cliente;
import com.dentia.service.interno.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteDocumentoVentaServiceTest {

    private ClienteService clienteService;
    private DocumentoExternoProvider provider;
    private ClienteDocumentoVentaService service;

    @BeforeEach
    void configurar() {
        clienteService = mock(ClienteService.class);
        provider = mock(DocumentoExternoProvider.class);
        service = new ClienteDocumentoVentaService(clienteService, provider);
    }

    @Test
    void clienteInternoActivoEvitaConsumirApi() {
        Cliente cliente = cliente(7, "12345678", true, 1, "Ana", "Torres", null);
        when(clienteService.buscarPorDocumento("12345678")).thenReturn(cliente);

        ClienteDocumentoConsultaDto resultado = service.consultar("12345678");

        assertEquals("BASE_INTERNA", resultado.origen());
        assertEquals(7, resultado.idCliente());
        assertEquals("Ana Torres", resultado.nombre());
        verifyNoInteractions(provider);
    }

    @Test
    void clienteInternoInactivoSeReutilizaSinReactivarlo() {
        Cliente cliente = cliente(8, "12345678", false, 1, "Ana", "Torres", null);
        when(clienteService.buscarPorDocumento("12345678")).thenReturn(cliente);

        ClienteDocumentoConsultaDto resultado = service.consultar("12345678");

        assertFalse(resultado.activo());
        assertTrue(resultado.mensaje().contains("inactivo"));
        verifyNoInteractions(provider);
    }

    @Test
    void rucInternoDevuelveRazonSocialReal() {
        Cliente cliente = cliente(9, "20131312955", true, 2, null, null, "Empresa Vial SAC");
        when(clienteService.buscarPorDocumento("20131312955")).thenReturn(cliente);

        ClienteDocumentoConsultaDto resultado = service.consultar("20131312955");

        assertEquals("RUC", resultado.tipoDocumento());
        assertEquals("Empresa Vial SAC", resultado.razonSocial());
        verifyNoInteractions(provider);
    }

    @Test
    void documentoNuevoUsaApiSinCrearClienteTodavia() {
        when(clienteService.buscarPorDocumento("12345678")).thenReturn(null);
        when(provider.consultar("12345678")).thenReturn(new DocumentoExternoResultadoDto(
                true, true, "Rosa", "Díaz Soto", "", "Av. Lima 100", "Encontrado"));

        ClienteDocumentoConsultaDto resultado = service.consultar("12345678");

        assertEquals("API_EXTERNA", resultado.origen());
        assertNull(resultado.idCliente());
        assertEquals("Rosa Díaz Soto", resultado.nombre());
    }

    @Test
    void apiSinResultadoHabilitaIngresoManual() {
        when(provider.consultar("12345678")).thenReturn(DocumentoExternoResultadoDto.noEncontrado());

        ClienteDocumentoConsultaDto resultado = service.consultar("12345678");

        assertEquals("NO_ENCONTRADO", resultado.origen());
        assertFalse(resultado.encontrado());
    }

    @Test
    void apiCaidaNoBloqueaIngresoManual() {
        when(provider.consultar("12345678")).thenReturn(
                DocumentoExternoResultadoDto.noDisponible("Completa manualmente"));

        ClienteDocumentoConsultaDto resultado = service.consultar("12345678");

        assertEquals("MANUAL", resultado.origen());
        assertFalse(resultado.encontrado());
    }

    @Test
    void apiSinSaldoNoBloqueaIngresoManual() {
        when(provider.consultar("12345678")).thenReturn(
                DocumentoExternoResultadoDto.noDisponible("La cuota de consultas no está disponible"));

        ClienteDocumentoConsultaDto resultado = service.consultar("12345678");

        assertEquals("MANUAL", resultado.origen());
        assertTrue(resultado.mensaje().contains("cuota"));
    }

    @Test
    void documentoInvalidoNoConsultaBaseNiApi() {
        assertThrows(IllegalArgumentException.class, () -> service.consultar("12A"));
        verifyNoInteractions(clienteService, provider);
    }

    private Cliente cliente(int id, String documento, boolean activo, int tipo,
                            String nombres, String apellidos, String razonSocial) {
        Cliente cliente = new Cliente();
        cliente.setIdClientes(id);
        cliente.setNro_documento(documento);
        cliente.setActivo(activo);
        cliente.setIdTipoDocumento(tipo);
        cliente.setNombres(nombres);
        cliente.setApellidos(apellidos);
        cliente.setRazonSocial(razonSocial);
        cliente.setTelefono("999888777");
        cliente.setCorreo("cliente@correo.pe");
        cliente.setDireccion("Av. Principal 123");
        return cliente;
    }
}
