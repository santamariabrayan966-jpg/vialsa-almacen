package com.dentia.service.interno;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dto.interno.ventas.CalculoMedidasRespuestaDto;
import com.dentia.dto.interno.ventas.CalculoMedidasSolicitudDto;
import com.dentia.model.interno.DetalleVenta;
import com.dentia.model.interno.Producto;
import com.dentia.model.interno.Venta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VentaMedidasConfirmacionServiceTest {

    @Test
    void confirmaSoloLineasActivasCuandoSeEliminaUnaLineaDelGrupo() {
        CalculoMedidasProvisionalService calculo = mock(CalculoMedidasProvisionalService.class);
        ProductoService productos = mock(ProductoService.class);
        VentaMedidasConfirmacionService service = new VentaMedidasConfirmacionService(
                calculo, productos, new ObjectMapper());

        Producto producto = new Producto();
        producto.setIdProducto(1);
        producto.setVentaPorMedida(true);
        producto.setTipoMedidaBase("AREA");
        when(productos.obtener(1)).thenReturn(producto);

        var lineaCompleta = linea(10, "Plancha completa", "UNIDAD", "1", "100", "1", true);
        var lineaPie = linea(12, "Pie²", "PIE2", "9", "5", "0.0131578947", false);
        var combinacion = new CalculoMedidasRespuestaDto.CombinacionDto(
                "COMBINACION-1", "Calculada", bd("145"), bd("1.2"), 2,
                List.of(lineaCompleta, lineaPie));
        when(calculo.calcular(any(CalculoMedidasSolicitudDto.class), eq(1))).thenReturn(
                new CalculoMedidasRespuestaDto(true, "AREA", "MM", null, plano(),
                        combinacion, List.of(), List.of(), List.of()));

        DetalleVenta principal = detalle(10, "1", "100", "1", true);
        principal.setCortesJson("""
                {
                  "modalidad":"COTIZAR_CORTES",
                  "codigoCombinacion":"COMBINACION-1",
                  "cortes":[{"ancho":300,"alto":300,"cantidad":1}],
                  "cortesCalculoOriginal":[
                    {"ancho":300,"alto":300,"cantidad":1},
                    {"ancho":200,"alto":200,"cantidad":1}
                  ],
                  "uiGrupoMedidas":{"lineas":[{
                    "idPrecioPresentacion":10,
                    "cantidad":1,
                    "precioUnitario":100,
                    "factorConversionStock":1,
                    "existente":true
                  }]}
                }
                """);

        Venta venta = new Venta();
        venta.setIdSede(1);
        venta.setDetalles(List.of(principal));

        service.validarYPreparar(venta);

        assertEquals(0, principal.getCantidadDescontadaStock().compareTo(bd("1.000000")));
        ArgumentCaptor<CalculoMedidasSolicitudDto> captor = ArgumentCaptor.forClass(CalculoMedidasSolicitudDto.class);
        verify(calculo).calcular(captor.capture(), eq(1));
        assertEquals(2, captor.getValue().cortes().size());
    }

    @Test
    void conservaCombinacionElegidaPermiteDescuentoYAgregaStockUnaSolaVez() {
        CalculoMedidasProvisionalService calculo = mock(CalculoMedidasProvisionalService.class);
        ProductoService productos = mock(ProductoService.class);
        VentaMedidasConfirmacionService service = new VentaMedidasConfirmacionService(
                calculo, productos, new ObjectMapper());

        Producto producto = new Producto();
        producto.setIdProducto(1);
        producto.setVentaPorMedida(true);
        producto.setTipoMedidaBase("AREA");
        when(productos.obtener(1)).thenReturn(producto);

        var lineaCompleta = linea(10, "Plancha completa", "UNIDAD", "1", "100", "1", true);
        var lineaPie = linea(12, "Pie²", "PIE2", "1", "5", "0.0131578947", false);
        var combinacion = new CalculoMedidasRespuestaDto.CombinacionDto(
                "RESIDUAL|elegida", "Elegida", bd("105"), bd("1.0929"), 2,
                List.of(lineaCompleta, lineaPie));
        var respuesta = new CalculoMedidasRespuestaDto(
                true, "AREA", "MM", null, plano(), combinacion, List.of(), List.of(), List.of());
        when(calculo.calcular(any(CalculoMedidasSolicitudDto.class), eq(1))).thenReturn(respuesta);

        DetalleVenta principal = detalle(10, "1", "100", "1", true);
        principal.setDescuento(bd("3"));
        principal.setCortesJson("""
                {"unidadEntrada":"MM","modalidad":"COTIZAR_CORTES",
                 "codigoCombinacion":"RESIDUAL|elegida",
                 "cortes":[{"ancho":300,"alto":300,"cantidad":1}]}
                """);
        DetalleVenta secundaria = detalle(12, "1", "5", "0.0131578947", false);

        Venta venta = new Venta();
        venta.setIdSede(1);
        venta.setDetalles(List.of(principal, secundaria));

        service.validarYPreparar(venta);

        assertEquals(0, principal.getCantidadDescontadaStock().compareTo(bd("1.013158")));
        assertEquals(0, secundaria.getCantidadDescontadaStock().compareTo(BigDecimal.ZERO));
        ArgumentCaptor<CalculoMedidasSolicitudDto> captor = ArgumentCaptor.forClass(CalculoMedidasSolicitudDto.class);
        verify(calculo).calcular(captor.capture(), eq(1));
        assertEquals("RESIDUAL|elegida", captor.getValue().codigoCombinacion());
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto plano() {
        var pieza = new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                1, "P-1", 10, "Plancha completa", 1, "COMPLETA",
                bd("1000"), bd("1000"), BigDecimal.ZERO,
                List.of(), List.of(), bd("1000000"), BigDecimal.ZERO, bd("100")
        );
        return new CalculoMedidasRespuestaDto.PlanoCorteDto("AREA", "MM", List.of(pieza));
    }

    private CalculoMedidasRespuestaDto.LineaComercialDto linea(
            int id, String nombre, String tipo, String cantidad, String precio, String factor, boolean principal) {
        BigDecimal cantidadBd = bd(cantidad);
        BigDecimal precioBd = bd(precio);
        return new CalculoMedidasRespuestaDto.LineaComercialDto(
                id, nombre, tipo, cantidadBd, precioBd, cantidadBd.multiply(precioBd),
                bd(factor), principal, List.of());
    }

    private DetalleVenta detalle(int idPresentacion, String cantidad, String precio,
                                 String factor, boolean principal) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setIdProducto(1);
        detalle.setIdPrecioPresentacion(idPresentacion);
        detalle.setCantidad(bd(cantidad));
        detalle.setPrecioUnitario(bd(precio));
        detalle.setFactorConversionStock(bd(factor));
        detalle.setDescuento(BigDecimal.ZERO);
        detalle.setGrupoMedidaUuid("grupo-1");
        detalle.setLineaPrincipalMedida(principal);
        return detalle;
    }

    private BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
