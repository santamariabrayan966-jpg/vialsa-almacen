package com.dentia.service.interno;

import com.dentia.dao.interfaces.IDetalleVentaDao;
import com.dentia.dao.interfaces.IVentaDao;
import com.dentia.dto.interno.ventas.ClienteVentaSolicitudDto;
import com.dentia.model.interno.Venta;
import com.dentia.model.interno.Cliente;
import com.dentia.service.interno.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VentaServiceClienteTest {

    private IVentaDao ventaDao;
    private ClienteService clienteService;
    private VentaService service;

    @BeforeEach
    void configurar() {
        ventaDao = mock(IVentaDao.class);
        clienteService = mock(ClienteService.class);
        service = new VentaService(
                ventaDao,
                mock(IDetalleVentaDao.class),
                mock(ProductoService.class),
                mock(InventarioService.class),
                mock(ProductoPrecioPresentacionService.class),
                clienteService);
    }

    @Test
    void guardarCabeceraVinculaIdYDatosPersistentesDelCliente() {
        Venta venta = venta("BOLETA", "12345678", "Dato navegador");
        Cliente cliente = new Cliente();
        cliente.setIdClientes(25);
        cliente.setNro_documento("12345678");
        cliente.setNombres("Nombre Interno");
        cliente.setApellidos("Real");
        cliente.setTelefono("999888777");
        when(clienteService.resolverClienteParaVenta(any(ClienteVentaSolicitudDto.class))).thenReturn(cliente);
        when(clienteService.nombreParaVenta(cliente)).thenReturn("Nombre Interno Real");
        when(ventaDao.registrarYObtenerId(any())).thenReturn(100);

        int idVenta = service.guardarBorradorConCliente(venta);

        assertEquals(100, idVenta);
        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaDao).registrarYObtenerId(captor.capture());
        assertEquals(25, captor.getValue().getIdCliente());
        assertEquals("Nombre Interno Real", captor.getValue().getNombreCliente());
        assertEquals("12345678", captor.getValue().getDocumentoCliente());
    }

    @Test
    void siFallaResolverClienteNoSeGuardaCabecera() {
        Venta venta = venta("FACTURA", "20131312955", "Empresa Real SAC");
        when(clienteService.resolverClienteParaVenta(any()))
                .thenThrow(new IllegalArgumentException("Falta dirección fiscal"));

        assertThrows(IllegalArgumentException.class, () -> service.guardarBorradorConCliente(venta));

        verify(ventaDao, never()).registrarYObtenerId(any());
        verify(ventaDao, never()).actualizarVenta(any());
    }

    @Test
    void notaSinDocumentoConservaReglaActualDeClienteOcasional() {
        Venta venta = venta("NOTA", "", "Cliente identificado verbalmente");
        when(ventaDao.registrarYObtenerId(any())).thenReturn(101);

        int id = service.guardarBorradorConCliente(venta);

        assertEquals(101, id);
        assertNull(venta.getIdCliente());
        verify(clienteService).validarNombreVenta("Cliente identificado verbalmente");
        verify(clienteService, never()).resolverClienteParaVenta(any());
    }

    private Venta venta(String comprobante, String documento, String nombre) {
        Venta venta = new Venta();
        venta.setTipoComprobante(comprobante);
        venta.setDocumentoCliente(documento);
        venta.setNombreCliente(nombre);
        venta.setTotalVenta(java.math.BigDecimal.TEN);
        venta.setDeuda(java.math.BigDecimal.ZERO);
        return venta;
    }
}
