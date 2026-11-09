package com.dentia.service.interno;

import com.dentia.dao.interfaces.IInventarioAjusteDao;
import com.dentia.dao.interfaces.IInventarioMovimientoDao;
import com.dentia.dao.interfaces.IInventarioParteDao;
import com.dentia.dao.interfaces.IInventarioReservaDao;
import com.dentia.dao.interfaces.IInventarioStockDao;
import com.dentia.dao.interfaces.IInventarioTransferenciaDao;
import com.dentia.dao.interfaces.IProductoDao;
import com.dentia.model.interno.inventario.InventarioStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventarioServiceVentaStockTest {

    private IInventarioStockDao stockDao;
    private IInventarioReservaDao reservaDao;
    private InventarioService service;

    @BeforeEach
    void setUp() {
        stockDao = mock(IInventarioStockDao.class);
        reservaDao = mock(IInventarioReservaDao.class);
        service = new InventarioService(
                stockDao,
                mock(IInventarioMovimientoDao.class),
                mock(IInventarioTransferenciaDao.class),
                mock(IInventarioAjusteDao.class),
                mock(IInventarioParteDao.class),
                reservaDao,
                mock(IProductoDao.class)
        );
    }

    @Test
    void permiteStockExactoDespuesDeReservas() {
        when(stockDao.bloquearPorSedeYProductos(3, List.of(7)))
                .thenReturn(List.of(stock(7, 3, "10")));
        when(reservaDao.sumarReservadoActivoPorProductoYSede(7, 3))
                .thenReturn(new BigDecimal("2"));

        assertDoesNotThrow(() -> service.bloquearYValidarStockVenta(
                3, Map.of(7, new BigDecimal("8"))));

        verify(stockDao).bloquearPorSedeYProductos(3, List.of(7));
    }

    @Test
    void rechazaStockInsuficienteConsiderandoReservas() {
        when(stockDao.bloquearPorSedeYProductos(3, List.of(7)))
                .thenReturn(List.of(stock(7, 3, "10")));
        when(reservaDao.sumarReservadoActivoPorProductoYSede(7, 3))
                .thenReturn(new BigDecimal("2.5"));

        assertThrows(RuntimeException.class, () -> service.bloquearYValidarStockVenta(
                3, Map.of(7, new BigDecimal("8"))));
    }

    @Test
    void bloqueaProductosEnOrdenFijoParaEvitarInterbloqueosConcurrentes() {
        when(stockDao.bloquearPorSedeYProductos(3, List.of(2, 9)))
                .thenReturn(List.of(stock(2, 3, "5"), stock(9, 3, "5")));
        when(reservaDao.sumarReservadoActivoPorProductoYSede(2, 3)).thenReturn(BigDecimal.ZERO);
        when(reservaDao.sumarReservadoActivoPorProductoYSede(9, 3)).thenReturn(BigDecimal.ZERO);

        service.bloquearYValidarStockVenta(3, Map.of(9, BigDecimal.ONE, 2, BigDecimal.ONE));

        verify(stockDao).bloquearPorSedeYProductos(3, List.of(2, 9));
    }

    private static InventarioStock stock(int idProducto, int idSede, String cantidad) {
        InventarioStock stock = new InventarioStock();
        stock.setIdProducto(idProducto);
        stock.setIdSede(idSede);
        stock.setStockActual(new BigDecimal(cantidad));
        return stock;
    }
}
