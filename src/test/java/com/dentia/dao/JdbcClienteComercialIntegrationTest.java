package com.dentia.dao;

import com.dentia.dao.interfaces.IClienteDao;
import com.dentia.dto.interno.clientes.ClienteComprasPaginaDto;
import com.dentia.dto.interno.clientes.ClientePaginaDto;
import com.dentia.dto.interno.clientes.ClienteResumenComercialDto;
import com.dentia.service.interno.ClienteService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("integration")
@SpringBootTest
class JdbcClienteComercialIntegrationTest {

    @Autowired private IClienteDao clienteDao;
    @Autowired private ClienteService clienteService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void clasificacionUsaSoloVentasValidasYNoDuplicaPedidos() {
        List<ClienteResumenComercialDto> resumenes = clienteDao.listarResumenesComerciales();
        Assumptions.assumeFalse(resumenes.isEmpty(), "No hay clientes para verificar");

        for (ClienteResumenComercialDto resumen : resumenes) {
            Long validas = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ventas
                     WHERE idClientes = ? AND UPPER(EstadoVenta) IN ('PAGADO', 'CREDITO')
                       AND NOT EXISTS (SELECT 1 FROM pedidos p INNER JOIN estadopedido ep ON ep.idEstadoPedido = p.idEstadoPedido
                                        WHERE p.idVenta = ventas.idVentas AND UPPER(ep.nombreEstado) = 'ANULADO')
                    """, Long.class, resumen.idCliente());
            Long online = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ventas
                     WHERE idClientes = ? AND UPPER(EstadoVenta) IN ('PAGADO', 'CREDITO')
                       AND UPPER(canal_venta) = 'ONLINE'
                       AND NOT EXISTS (SELECT 1 FROM pedidos p INNER JOIN estadopedido ep ON ep.idEstadoPedido = p.idEstadoPedido
                                        WHERE p.idVenta = ventas.idVentas AND UPPER(ep.nombreEstado) = 'ANULADO')
                    """, Long.class, resumen.idCliente());
            Long presencial = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ventas
                     WHERE idClientes = ? AND UPPER(EstadoVenta) IN ('PAGADO', 'CREDITO')
                       AND UPPER(canal_venta) = 'PRESENCIAL'
                       AND NOT EXISTS (SELECT 1 FROM pedidos p INNER JOIN estadopedido ep ON ep.idEstadoPedido = p.idEstadoPedido
                                        WHERE p.idVenta = ventas.idVentas AND UPPER(ep.nombreEstado) = 'ANULADO')
                    """, Long.class, resumen.idCliente());

            String esperada = validas == 0 ? "SIN COMPRAS"
                    : online > 0 && presencial > 0 ? "MIXTO"
                    : online > 0 ? "ONLINE"
                    : presencial > 0 ? "PRESENCIAL" : "SIN COMPRAS";
            assertEquals(validas, resumen.cantidadCompras());
            assertEquals(esperada, resumen.clasificacion());
        }
    }

    @Test
    void historialEsPaginadoYRankingRespetaLimites() {
        List<ClienteResumenComercialDto> conCompras = clienteDao.listarResumenesComerciales().stream()
                .filter(resumen -> resumen.cantidadCompras() > 0)
                .toList();
        Assumptions.assumeFalse(conCompras.isEmpty(), "No hay clientes con compras válidas");

        Integer idCliente = conCompras.get(0).idCliente();
        ClienteComprasPaginaDto pagina = clienteService.obtenerComprasCliente(
                idCliente, "", "TODOS", "TODOS", null,
                null, null, null, null, null, 0, 5);
        assertTrue(pagina.compras().size() <= 5);
        assertEquals(conCompras.get(0).cantidadCompras(), pagina.totalElementos());
        assertNotNull(clienteService.obtenerDetalleComercial(idCliente));

        assertTrue(clienteService.obtenerRanking(null, null, "TODOS", null, "MONTO", 10).size() <= 10);
        assertTrue(clienteService.obtenerRanking(null, null, "TODOS", null, "COMPRAS", 20).size() <= 20);
    }

    @Test
    void listadoPrincipalUsaPaginasDeDiezYOrdenDescendente() {
        ClientePaginaDto primera = clienteService.obtenerPagina("", "TODOS", 0, 10);
        assertTrue(primera.clientes().size() <= 10);
        assertEquals(clienteDao.contarClientes("", "TODOS"), primera.totalElementos());
        assertEquals(10, primera.tamano());

        for (int i = 1; i < primera.clientes().size(); i++) {
            var anterior = primera.clientes().get(i - 1);
            var actual = primera.clientes().get(i);
            if (anterior.getFechaRegistro() != null && actual.getFechaRegistro() != null
                    && anterior.getFechaRegistro().equals(actual.getFechaRegistro())) {
                assertTrue(anterior.getIdClientes() > actual.getIdClientes());
            } else if (anterior.getFechaRegistro() != null && actual.getFechaRegistro() != null) {
                assertTrue(!anterior.getFechaRegistro().before(actual.getFechaRegistro()));
            }
        }

        if (primera.totalElementos() > 10) {
            ClientePaginaDto segunda = clienteService.obtenerPagina("", "TODOS", 1, 10);
            assertEquals(1, segunda.pagina());
            assertTrue(segunda.clientes().stream().noneMatch(c -> primera.clientes().stream()
                    .anyMatch(p -> p.getIdClientes().equals(c.getIdClientes()))));
        }
    }

    @Test
    void fechaExactaTienePrioridadYConservaAgregadosFiltrados() {
        List<ClienteResumenComercialDto> conCompras = clienteDao.listarResumenesComerciales().stream()
                .filter(resumen -> resumen.cantidadCompras() > 0)
                .toList();
        Assumptions.assumeFalse(conCompras.isEmpty(), "No hay clientes con compras válidas");
        Integer idCliente = conCompras.get(0).idCliente();
        ClienteComprasPaginaDto muestra = clienteService.obtenerComprasCliente(
                idCliente, "", "TODOS", "TODOS", null,
                null, null, null, null, null, 0, 10);
        Assumptions.assumeFalse(muestra.compras().isEmpty(), "No hay compras para seleccionar una fecha");
        var fecha = muestra.compras().get(0).fecha().toLocalDate();
        int mesDistinto = fecha.getMonthValue() == 12 ? 1 : fecha.getMonthValue() + 1;

        ClienteComprasPaginaDto exacta = clienteService.obtenerComprasCliente(
                idCliente, "", "TODOS", "TODOS", null,
                fecha, mesDistinto, fecha.getYear() - 1, fecha.minusYears(2), fecha.minusYears(1), 0, 10);
        assertTrue(exacta.compras().stream().allMatch(compra -> compra.fecha().toLocalDate().equals(fecha)));
        assertEquals(exacta.totalElementos(), exacta.comprasFiltradas());
        assertTrue(exacta.totalElementos() >= exacta.compras().size());
    }
}
