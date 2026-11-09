package com.dentia.service.interno;

import com.dentia.dao.interfaces.IProveedorDao;
import com.dentia.dto.interno.ProveedorPaginaDto;
import com.dentia.model.interno.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProveedorServiceTest {

    private IProveedorDao dao;
    private ProveedorService service;

    @BeforeEach
    void setUp() {
        dao = mock(IProveedorDao.class);
        service = new ProveedorService(dao);
    }

    @ParameterizedTest
    @CsvSource({"4,1", "10,1", "11,2", "100,10"})
    void calculaPaginasParaDistintosVolumenes(long total, int paginasEsperadas) {
        when(dao.contar("")).thenReturn(total);
        when(dao.listarPaginado("", 10, 0)).thenReturn(List.of());

        ProveedorPaginaDto pagina = service.obtenerPagina(0, 10, "");

        assertEquals(total, pagina.totalElementos());
        assertEquals(paginasEsperadas, pagina.totalPaginas());
        assertEquals(10, pagina.tamanio());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 20, 50})
    void admiteLosTamaniosConfigurados(int tamanio) {
        when(dao.contar("dentia")).thenReturn(100L);
        when(dao.listarPaginado("dentia", tamanio, tamanio)).thenReturn(List.of());

        ProveedorPaginaDto pagina = service.obtenerPagina(1, tamanio, "  dentia  ");

        assertEquals(tamanio, pagina.tamanio());
        assertEquals("dentia", pagina.busqueda());
        verify(dao).listarPaginado("dentia", tamanio, tamanio);
    }

    @Test
    void usaDiezSiElTamanioNoEstaPermitidoYAjustaPaginaFueraDeRango() {
        when(dao.contar("")).thenReturn(100L);
        when(dao.listarPaginado("", 10, 90)).thenReturn(List.of());

        ProveedorPaginaDto pagina = service.obtenerPagina(999, 100, "");

        assertEquals(9, pagina.pagina());
        assertEquals(10, pagina.tamanio());
        verify(dao).listarPaginado("", 10, 90);
    }

    @Test
    void creaProveedorValidoNormalizandoCampos() {
        Proveedor proveedor = valido();
        proveedor.setNombreProveedor("  Inversiones   & Servicios del Norte E.I.R.L.  ");
        proveedor.setCorreo(" VENTAS@EMPRESA.COM ");
        when(dao.buscarPorNroDocumento("20123456789")).thenReturn(Optional.empty());
        when(dao.crear(any(Proveedor.class))).thenReturn(1);

        ProveedorService.ResultadoOperacion resultado = service.guardar(proveedor);

        assertTrue(resultado.exitoso());
        assertEquals(ProveedorService.ResultadoGuardarProveedor.NUEVO, resultado.resultado());
        assertEquals("Inversiones & Servicios del Norte E.I.R.L.", proveedor.getNombreProveedor());
        assertEquals("ventas@empresa.com", proveedor.getCorreo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234567", "123456789", "1234567890", "123456789012", "12A45678"})
    void rechazaDocumentosInvalidos(String documento) {
        Proveedor proveedor = valido();
        proveedor.setNroDocumento(documento);

        ProveedorService.ResultadoOperacion resultado = service.guardar(proveedor);

        assertEquals(ProveedorService.ResultadoGuardarProveedor.INVALIDO, resultado.resultado());
        assertTrue(resultado.errores().containsKey("nroDocumento"));
        verify(dao, never()).crear(any());
    }

    @Test
    void rechazaNombreRepetitivoONumerico() {
        Proveedor proveedor = valido();
        proveedor.setNombreProveedor("aaa");
        assertTrue(service.guardar(proveedor).errores().containsKey("nombreProveedor"));

        proveedor.setNombreProveedor("123");
        assertTrue(service.guardar(proveedor).errores().containsKey("nombreProveedor"));
    }

    @Test
    void impideDocumentoDuplicadoEnAlta() {
        Proveedor existente = existente(9, 1, "20123456789");
        when(dao.buscarPorNroDocumento("20123456789")).thenReturn(Optional.of(existente));

        ProveedorService.ResultadoOperacion resultado = service.guardar(valido());

        assertEquals(ProveedorService.ResultadoGuardarProveedor.DUPLICADO, resultado.resultado());
        verify(dao, never()).crear(any());
    }

    @Test
    void permiteEditarConElMismoDocumentoYPreservaEstadoInactivo() {
        Proveedor proveedor = valido();
        proveedor.setIdProveedor(4);
        Proveedor existente = existente(4, 0, "20123456789");
        when(dao.buscarPorId(4)).thenReturn(Optional.of(existente));
        when(dao.buscarPorNroDocumento("20123456789")).thenReturn(Optional.of(existente));
        when(dao.actualizar(any(Proveedor.class))).thenReturn(1);

        ProveedorService.ResultadoOperacion resultado = service.guardar(proveedor);

        assertEquals(ProveedorService.ResultadoGuardarProveedor.ACTUALIZADO, resultado.resultado());
        ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
        verify(dao).actualizar(captor.capture());
        assertEquals(0, captor.getValue().getEstado());
        assertFalse(captor.getValue().isActivo());
    }

    @Test
    void impideUsarDocumentoDeOtroProveedorAlEditar() {
        Proveedor proveedor = valido();
        proveedor.setIdProveedor(4);
        when(dao.buscarPorId(4)).thenReturn(Optional.of(existente(4, 1, "87654321")));
        when(dao.buscarPorNroDocumento("20123456789"))
                .thenReturn(Optional.of(existente(8, 1, "20123456789")));

        ProveedorService.ResultadoOperacion resultado = service.guardar(proveedor);

        assertEquals(ProveedorService.ResultadoGuardarProveedor.DUPLICADO, resultado.resultado());
        verify(dao, never()).actualizar(any());
    }

    @Test
    void noEditaProveedorEliminadoLogicamente() {
        Proveedor proveedor = valido();
        proveedor.setIdProveedor(4);
        Proveedor eliminado = existente(4, 2, "20123456789");
        when(dao.buscarPorId(4)).thenReturn(Optional.of(eliminado));
        when(dao.buscarPorNroDocumento("20123456789")).thenReturn(Optional.of(eliminado));

        ProveedorService.ResultadoOperacion resultado = service.guardar(proveedor);

        assertEquals(ProveedorService.ResultadoGuardarProveedor.ELIMINADO, resultado.resultado());
        verify(dao, never()).actualizar(any());
    }

    @Test
    void validaTelefonoCorreoYDireccion() {
        Proveedor proveedor = valido();
        proveedor.setTelefono("123456789");
        proveedor.setCorreo("correo@dominio");
        proveedor.setDireccion("12345");

        ProveedorService.ResultadoOperacion resultado = service.guardar(proveedor);

        assertTrue(resultado.errores().containsKey("telefono"));
        assertTrue(resultado.errores().containsKey("correo"));
        assertTrue(resultado.errores().containsKey("direccion"));
    }

    private Proveedor valido() {
        Proveedor proveedor = new Proveedor();
        proveedor.setNroDocumento("20123456789");
        proveedor.setNombreProveedor("DentIA S.A.C.");
        proveedor.setTelefono("987654321");
        proveedor.setCorreo("ventas@dentia.com");
        proveedor.setDireccion("Av. Principal 123");
        return proveedor;
    }

    private Proveedor existente(int id, int estado, String documento) {
        Proveedor proveedor = valido();
        proveedor.setIdProveedor(id);
        proveedor.setEstado(estado);
        proveedor.setNroDocumento(documento);
        return proveedor;
    }
}
