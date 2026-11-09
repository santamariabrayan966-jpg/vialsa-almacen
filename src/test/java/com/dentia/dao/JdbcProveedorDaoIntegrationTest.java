package com.dentia.dao;

import com.dentia.dao.interfaces.IProveedorDao;
import com.dentia.dto.interno.ProveedorComprasResponseDto;
import com.dentia.model.interno.Proveedor;
import com.dentia.service.interno.ProveedorService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.junit.jupiter.api.Tag("integration")
@SpringBootTest
class JdbcProveedorDaoIntegrationTest {

    @Autowired
    private IProveedorDao proveedorDao;

    @Autowired
    private ProveedorService proveedorService;

    @Test
    void historialEsCompatibleConElEsquemaRealSinModificarDatos() {
        List<Proveedor> proveedores = proveedorDao.listar();
        Assumptions.assumeFalse(proveedores.isEmpty(), "No hay proveedores para verificar el historial");

        Integer idProveedor = proveedores.get(0).getIdProveedor();
        assertNotNull(proveedorDao.obtenerResumenCompras(idProveedor));
        assertNotNull(proveedorDao.obtenerComprasPorSede(idProveedor));
        assertNotNull(proveedorDao.listarEstadosCompras(idProveedor));

        ProveedorComprasResponseDto historial = proveedorService.obtenerHistorial(
                idProveedor, "", null, "", null, null, 0, 8);
        assertNotNull(historial);
        assertTrue(historial.totalElementos() >= 0);
        assertTrue(historial.compras().size() <= 8);
    }

    @Test
    void listadoPaginadoYBusquedaFuncionanConElEsquemaReal() {
        long total = proveedorDao.contar("");
        List<Proveedor> primeraPagina = proveedorDao.listarPaginado("", 10, 0);

        assertTrue(primeraPagina.size() <= 10);
        assertTrue(total >= primeraPagina.size());

        if (!primeraPagina.isEmpty()) {
            String documento = primeraPagina.get(0).getNroDocumento();
            assertTrue(proveedorDao.contar(documento) >= 1);
            assertTrue(proveedorDao.listarPaginado(documento, 5, 0).stream()
                    .anyMatch(proveedor -> documento.equals(proveedor.getNroDocumento())));
        }
    }
}
