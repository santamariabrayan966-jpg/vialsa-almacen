package com.dentia.service.interno;

import com.dentia.model.interno.DetalleVenta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VentaServiceGrupoMedidasTest {

    private final VentaService service = new VentaService(null, null, null, null, null, null);

    @Test
    void aceptaGrupoConUnaPrincipalYSecundariaSinJsonDuplicado() {
        DetalleVenta principal = detalle("grupo-1", true, 10);
        principal.setCortesJson("{\"unidadEntrada\":\"MM\",\"cortes\":[{\"ancho\":500,\"alto\":500,\"cantidad\":2}]}");
        principal.setCroquisSvg("<svg></svg>");
        DetalleVenta secundaria = detalle("grupo-1", false, 10);

        assertDoesNotThrow(() -> service.validarGruposMedidas(List.of(principal, secundaria)));
    }

    @Test
    void rechazaGrupoConDosPrincipales() {
        DetalleVenta primera = detalle("grupo-1", true, 10);
        primera.setCortesJson("[]");
        DetalleVenta segunda = detalle("grupo-1", true, 10);
        segunda.setCortesJson("[]");

        assertThrows(RuntimeException.class,
                () -> service.validarGruposMedidas(List.of(primera, segunda)));
    }

    @Test
    void rechazaProductosDistintosEnElMismoGrupo() {
        DetalleVenta principal = detalle("grupo-1", true, 10);
        principal.setCortesJson("[]");
        DetalleVenta secundaria = detalle("grupo-1", false, 11);

        assertThrows(RuntimeException.class,
                () -> service.validarGruposMedidas(List.of(principal, secundaria)));
    }

    @Test
    void ventaNormalSinMedidasSigueSiendoValida() {
        DetalleVenta normal = detalle(null, false, 10);

        assertDoesNotThrow(() -> service.validarGruposMedidas(List.of(normal)));
    }

    private DetalleVenta detalle(String grupo, boolean principal, int producto) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setGrupoMedidaUuid(grupo);
        detalle.setLineaPrincipalMedida(principal);
        detalle.setIdProducto(producto);
        return detalle;
    }
}
