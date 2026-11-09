package com.dentia.service.interno;

import com.dentia.dao.interfaces.IClienteDao;
import com.dentia.dto.interno.ventas.ClienteVentaSolicitudDto;
import com.dentia.model.interno.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteServiceVentaTest {

    private IClienteDao dao;
    private ClienteService service;

    @BeforeEach
    void configurar() {
        dao = mock(IClienteDao.class);
        service = new ClienteService(dao);
    }

    @Test
    void reutilizaClienteExistenteYNoSobrescribeDatosInternos() {
        Cliente existente = cliente(5, "12345678", 1, "Nombre Interno", "Real", null, "Calle real");
        when(dao.buscarPorDocumentoParaActualizar("12345678")).thenReturn(existente);
        when(dao.buscarPorId(5)).thenReturn(existente);

        Cliente resultado = service.resolverClienteParaVenta(solicitud(
                5, "12345678", "Nombre API", "BOLETA", "API_EXTERNA", "Calle externa"));

        assertSame(existente, resultado);
        assertEquals("Nombre Interno", resultado.getNombres());
        verify(dao, never()).registrar(any());
    }

    @Test
    void rechazaIdClienteQueNoCorrespondeAlDocumento() {
        Cliente porId = cliente(5, "87654321", 1, "Ana", "Real", null, "Calle real");
        when(dao.buscarPorId(5)).thenReturn(porId);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.resolverClienteParaVenta(solicitud(
                        5, "12345678", "Ana Real", "BOLETA", "BASE_INTERNA", "Calle real")));

        assertTrue(error.getMessage().contains("no corresponde"));
    }

    @Test
    void creaClienteDniValidadoDesdeApiOManual() {
        Cliente creado = cliente(12, "12345678", 1, "Rosa Díaz", "", null, "Av. Lima 100");
        when(dao.buscarPorDocumentoParaActualizar("12345678")).thenReturn(null, creado);

        Cliente resultado = service.resolverClienteParaVenta(solicitud(
                null, "12345678", "Rosa Díaz", "BOLETA", "API_EXTERNA", "Av. Lima 100"));

        assertEquals(12, resultado.getIdClientes());
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(dao).registrar(captor.capture());
        assertEquals(1, captor.getValue().getIdTipoDocumento());
        assertEquals("Rosa Díaz", captor.getValue().getNombres());
    }

    @Test
    void creaClienteRucManualConDatosFiscales() {
        Cliente creado = cliente(13, "20131312955", 2, null, null, "Empresa Real SAC", "Av. Fiscal 200");
        when(dao.buscarPorDocumentoParaActualizar("20131312955")).thenReturn(null, creado);
        ClienteVentaSolicitudDto solicitud = new ClienteVentaSolicitudDto(
                null, "20131312955", "", "", "Empresa Real SAC", "Empresa Real SAC",
                "999888777", "empresa@correo.pe", "Av. Fiscal 200", "FACTURA", "MANUAL");

        Cliente resultado = service.resolverClienteParaVenta(solicitud);

        assertEquals(13, resultado.getIdClientes());
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(dao).registrar(captor.capture());
        assertEquals(2, captor.getValue().getIdTipoDocumento());
        assertEquals("Empresa Real SAC", captor.getValue().getRazonSocial());
        assertEquals("Av. Fiscal 200", captor.getValue().getDireccion());
    }

    @Test
    void duplicadoConcurrenteSeReconsultaYReutiliza() {
        Cliente concurrente = cliente(18, "12345678", 1, "Rosa", "Díaz", null, "Av. Lima 100");
        when(dao.buscarPorDocumentoParaActualizar("12345678")).thenReturn(null, concurrente);
        doThrow(new DuplicateKeyException("duplicado")).when(dao).registrar(any());

        Cliente resultado = service.resolverClienteParaVenta(solicitud(
                null, "12345678", "Rosa Díaz", "BOLETA", "MANUAL", "Av. Lima 100"));

        assertSame(concurrente, resultado);
    }

    @Test
    void facturaExigeRucRazonSocialYDireccionFiscal() {
        Cliente rucSinDireccion = cliente(20, "20131312955", 2, null, null, "Empresa Real SAC", null);
        when(dao.buscarPorDocumentoParaActualizar("20131312955")).thenReturn(rucSinDireccion);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.resolverClienteParaVenta(solicitud(
                        null, "20131312955", "Empresa Real SAC", "FACTURA", "BASE_INTERNA", "")));

        assertTrue(error.getMessage().contains("dirección fiscal"));
    }

    @Test
    void boletaRechazaDocumentoInvalidoYNombreFicticio() {
        assertThrows(IllegalArgumentException.class, () -> service.resolverClienteParaVenta(solicitud(
                null, "123", "Ana Real", "BOLETA", "MANUAL", "Calle")));

        when(dao.buscarPorDocumentoParaActualizar("12345678")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.resolverClienteParaVenta(solicitud(
                null, "12345678", "Cliente", "BOLETA", "MANUAL", "Calle")));
    }

    private ClienteVentaSolicitudDto solicitud(Integer id, String documento, String nombre,
                                                String comprobante, String origen, String direccion) {
        return new ClienteVentaSolicitudDto(id, documento, nombre, "", nombre, nombre,
                "999888777", "real@correo.pe", direccion, comprobante, origen);
    }

    private Cliente cliente(int id, String documento, int tipo, String nombres,
                            String apellidos, String razonSocial, String direccion) {
        Cliente cliente = new Cliente();
        cliente.setIdClientes(id);
        cliente.setNro_documento(documento);
        cliente.setIdTipoDocumento(tipo);
        cliente.setNombres(nombres);
        cliente.setApellidos(apellidos);
        cliente.setRazonSocial(razonSocial);
        cliente.setDireccion(direccion);
        cliente.setActivo(true);
        return cliente;
    }
}
