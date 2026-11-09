package com.dentia.service.interno;

import com.dentia.dto.interno.ventas.CalculoMedidasRespuestaDto;
import com.dentia.dto.interno.ventas.CalculoMedidasSolicitudDto;
import com.dentia.model.interno.Producto;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalculoMedidasProvisionalServiceTest {

    private ProductoPrecioPresentacionService presentacionService;
    private InventarioService inventarioService;
    private CalculoMedidasProvisionalService service;
    private ProductoPrecioPresentacion completa;
    private ProductoPrecioPresentacion media;
    private ProductoPrecioPresentacion pie2;

    @BeforeEach
    void setUp() {
        ProductoService productoService = mock(ProductoService.class);
        presentacionService = mock(ProductoPrecioPresentacionService.class);
        inventarioService = mock(InventarioService.class);
        service = new CalculoMedidasProvisionalService(productoService, presentacionService, inventarioService);

        Producto producto = new Producto();
        producto.setIdProducto(1);
        producto.setIdEstadoProducto(1);
        producto.setVentaPorMedida(true);
        producto.setTipoMedidaBase("AREA");
        producto.setLargoBase(bd("2.509"));
        producto.setAnchoBase(bd("2.1"));
        when(productoService.obtener(1)).thenReturn(producto);

        completa = presentacion(10, "Plancha completa", "UNIDAD", "100", "1", "2.509", "2.1");
        media = presentacion(11, "Media plancha", "MEDIA", "60", "0.5", "1.2545", "2.1");
        pie2 = presentacion(12, "Pie²", "PIE2", "5", "0.0176323407162785401127370039", null, null);
        stock("20");
    }

    @Test
    void cortePequenoSeCobraComoUnPieCuadrado() {
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(corte("300", "300", 1)));

        var linea = respuesta.combinacionRecomendada().lineas().get(0);
        assertEquals("PIE2", linea.tipoCalculoVenta());
        assertEquals(0, linea.cantidad().compareTo(BigDecimal.ONE));
        assertEquals(List.of(1), linea.numerosCorte());
        assertEquals("COMERCIAL_MEDIDA", respuesta.planoCorte().piezas().get(0).tipoPlano());
    }

    @Test
    void metroCuadradoConservaCantidadDecimalSinRedondearAUnidadEntera() {
        var m2 = presentacion(13, "Metro cuadrado", "M2", "10", "0.1897929359069255442312437131", null, null);
        completa.setPrecioVenta(bd("100"));
        when(presentacionService.listarActivasPorProductoYSede(1, 1))
                .thenReturn(List.of(completa, m2));

        var respuesta = calcular(cortes(corte("300", "300", 1)));

        var linea = respuesta.combinacionRecomendada().lineas().get(0);
        assertEquals("M2", linea.tipoCalculoVenta());
        assertEquals(0, linea.cantidad().compareTo(bd("0.090000")));
        assertEquals(0, linea.subtotal().compareTo(bd("0.90")));
    }

    @Test
    void variosCortesQueCabenEligenMediaPlancha() {
        configurar("100", "20", "100");
        var respuesta = calcular(cortes(corte("500", "500", 4)));

        assertEquals(11, respuesta.combinacionRecomendada().lineas().get(0).idPrecioPresentacion());
        assertEquals(1, respuesta.combinacionRecomendada().piezasFisicas());
    }

    @Test
    void combinaPlanchaCompletaYMediaPlancha() {
        configurar("100", "60", "100");
        var respuesta = calcular(cortes(
                corte("2500", "2000", 1),
                corte("1250", "2000", 1)));

        assertEquals(2, respuesta.combinacionRecomendada().lineas().size());
        assertTrue(ids(respuesta).containsAll(List.of(10, 11)));
    }

    @Test
    void combinaCompletaMediaYUnPieResidualConNumerosOriginales() {
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(
                corte("2500", "2000", 1),
                corte("1250", "2000", 1),
                corte("300", "300", 1)));

        assertTrue(respuesta.combinacionRecomendada().codigo().startsWith("RESIDUAL|"));
        var residual = respuesta.combinacionRecomendada().lineas().stream()
                .filter(l -> "PIE2".equals(l.tipoCalculoVenta())).findFirst().orElseThrow();
        assertEquals(BigDecimal.ONE, residual.cantidad());
        assertEquals(List.of(3), residual.numerosCorte());
        assertEquals(1, respuesta.combinacionRecomendada().lineas().stream()
                .filter(CalculoMedidasRespuestaDto.LineaComercialDto::lineaPrincipal).count());

        var planoResidual = respuesta.planoCorte().piezas().stream()
                .filter(p -> "COMERCIAL_MEDIDA".equals(p.tipoPlano())).findFirst().orElseThrow();
        assertEquals(List.of(3), planoResidual.cortes().stream()
                .map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero).toList());
        assertEquals(List.of(1, 2), respuesta.planoCorte().piezas().stream()
                .filter(p -> "FISICO".equals(p.tipoPlano()))
                .flatMap(p -> p.cortes().stream())
                .map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero).sorted().toList());
    }

    @Test
    void conservaMediaCuandoPieCuadradoEsMasCaro() {
        configurar("100", "60", "100");
        var respuesta = calcular(cortes(
                corte("2500", "2000", 1),
                corte("1250", "2000", 1),
                corte("300", "300", 1)));

        assertFalse(respuesta.combinacionRecomendada().codigo().startsWith("RESIDUAL|"));
        assertFalse(ids(respuesta).contains(12));
    }

    @Test
    void presentacionFijaNoEsSustituida() {
        configurar("100", "20", "1");
        var solicitud = solicitud("COMPRAR_PRESENTACION", 10, 1,
                cortes(corte("300", "300", 1)));
        var respuesta = service.calcular(solicitud, 1);

        assertEquals("FIJA-10x1", respuesta.combinacionRecomendada().codigo());
        assertEquals(List.of(10), ids(respuesta));
        assertEquals(BigDecimal.ONE, respuesta.combinacionRecomendada().lineas().get(0).cantidad());
    }

    @Test
    void rechazaCortesQueNoCabenEnPresentacionFija() {
        configurar("100", "20", "1");
        var solicitud = solicitud("COMPRAR_PRESENTACION", 11, 1,
                cortes(corte("2500", "2000", 1)));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.calcular(solicitud, 1));
        assertTrue(ex.getMessage().contains("no cabe individualmente"));
    }

    @Test
    void rechazaStockInsuficienteEnPresentacionFija() {
        stock("0.4");
        configurar("100", "20", "1");
        var solicitud = solicitud("COMPRAR_PRESENTACION", 11, 1,
                cortes(corte("500", "500", 1)));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.calcular(solicitud, 1));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void rechazaGrupoNuloComoSolicitudInvalida() {
        configurar("100", "20", "1");
        var solicitud = solicitud("COTIZAR_CORTES", null, null,
                java.util.Arrays.asList((CalculoMedidasSolicitudDto.CorteMedidaDto) null));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.calcular(solicitud, 1));
        assertTrue(ex.getMessage().contains("datos válidos"));
    }

    @Test
    void reconoceMediaPorTipoTecnicoSinDependerDelNombre() {
        media.setNombrePresentacion("Mitad especial");
        media.setLargoBase(null);
        media.setAnchoBase(null);
        configurar("100", "20", "100");

        var respuesta = calcular(cortes(corte("500", "500", 4)));

        assertEquals(11, respuesta.combinacionRecomendada().lineas().get(0).idPrecioPresentacion());
    }

    @Test
    void residualNoDuplicaConsumoDePiezaRetirada() {
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(
                corte("2500", "2000", 1),
                corte("1250", "2000", 1),
                corte("300", "300", 1)));
        BigDecimal consumo = respuesta.combinacionRecomendada().lineas().stream()
                .map(l -> l.cantidad().multiply(l.factorConversionStock()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, consumo.compareTo(
                BigDecimal.ONE.add(bd("0.5")).add(pie2.getFactorConversionStock())));
    }

    @Test
    void casoRealMantieneResidualDeTrescientosPorTrescientosEnUnPie() {
        media.setLargoBase(bd("2.509"));
        media.setAnchoBase(bd("1.05"));
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(
                corte("2500", "1000", 1),
                corte("2509", "2100", 1),
                corte("300", "1000", 6),
                corte("300", "300", 1)));

        List<CalculoMedidasRespuestaDto.CombinacionDto> opciones = new java.util.ArrayList<>();
        opciones.add(respuesta.combinacionRecomendada());
        opciones.addAll(respuesta.alternativas());
        var residual = opciones.stream()
                .filter(c -> c.codigo().startsWith("RESIDUAL|"))
                .flatMap(c -> c.lineas().stream())
                .filter(l -> "PIE2".equals(l.tipoCalculoVenta()) && l.numerosCorte().equals(List.of(9)))
                .findFirst().orElseThrow();

        assertEquals(0, residual.cantidad().compareTo(BigDecimal.ONE));
    }

    @Test
    void comprarPresentacionCalculaDosPlanchasYDosPlanos() {
        configurar("100", "90", "100");
        var solicitud = solicitud("COMPRAR_PRESENTACION", 10, 1,
                cortes(corte("2500", "2000", 2)));

        var respuesta = service.calcular(solicitud, 1);

        assertEquals(0, respuesta.combinacionRecomendada().lineas().get(0).cantidad().compareTo(bd("2")));
        assertEquals(2, respuesta.planoCorte().piezas().size());
        assertEquals(List.of(1, 2), respuesta.planoCorte().piezas().stream()
                .flatMap(p -> p.cortes().stream()).map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero)
                .sorted().toList());
    }

    @Test
    void comprarPresentacionPermiteAumentarPeroNoBajaDelMinimo() {
        configurar("100", "90", "100");
        var solicitud = solicitud("COMPRAR_PRESENTACION", 10, 3,
                cortes(corte("2500", "2000", 2)));

        var respuesta = service.calcular(solicitud, 1);

        assertEquals(0, respuesta.combinacionRecomendada().lineas().get(0).cantidad().compareTo(bd("3")));
        assertEquals(3, respuesta.planoCorte().piezas().size());
    }

    @Test
    void stockInsuficienteInformaCantidadRequeridaYDisponible() {
        stock("1.5");
        configurar("100", "90", "100");
        var solicitud = solicitud("COMPRAR_PRESENTACION", 10, 1,
                cortes(corte("2500", "2000", 2)));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.calcular(solicitud, 1));

        assertTrue(ex.getMessage().contains("Cantidad requerida: 2"));
        assertTrue(ex.getMessage().contains("cantidad disponible: 1"));
    }

    @Test
    void comprarPresentacionAceptaTodasLasConfiguradasIncluidaVentaPorMedida() {
        configurar("100", "60", "5");

        for (Integer id : List.of(10, 11, 12)) {
            var respuesta = service.calcular(solicitud("COMPRAR_PRESENTACION", id, 1,
                    cortes(corte("300", "300", 1))), 1);
            assertEquals(id, respuesta.combinacionRecomendada().lineas().get(0).idPrecioPresentacion());
        }
    }

    @Test
    void fusionaFranjasContiguasEnUnSoloSobrante() {
        completa.setLargoBase(bd("3.3"));
        completa.setAnchoBase(bd("0.9"));
        configurar("100", "200", "1000");

        var respuesta = calcular(cortes(corte("2000", "300", 3)));

        var plano = respuesta.planoCorte().piezas().get(0);
        assertEquals(1, plano.sobrantes().size());
        assertEquals(0, plano.sobrantes().get(0).ancho().compareTo(bd("1300")));
        assertEquals(0, plano.sobrantes().get(0).alto().compareTo(bd("900")));
    }

    @Test
    void reintentaOrdenYColocaCorteEnRegionFusionadaSinMandarloAPie2() {
        completa.setLargoBase(bd("3.3"));
        completa.setAnchoBase(bd("0.9"));
        configurar("100", "200", "1000");

        var respuesta = calcular(cortes(
                corte("2000", "300", 3),
                corte("1200", "800", 1)));

        assertEquals(List.of(10), ids(respuesta));
        assertEquals(List.of(1, 2, 3, 4), respuesta.planoCorte().piezas().stream()
                .flatMap(p -> p.cortes().stream()).map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero)
                .sorted().toList());
    }

    @Test
    void sobrantesNoSeSuperponenYConservanElAreaLibre() {
        configurar("100", "200", "1000");
        var respuesta = calcular(cortes(
                corte("1000", "500", 1),
                corte("700", "400", 1),
                corte("300", "300", 1)));
        var pieza = respuesta.planoCorte().piezas().get(0);

        for (int i = 0; i < pieza.sobrantes().size(); i++) {
            for (int j = i + 1; j < pieza.sobrantes().size(); j++) {
                var a = pieza.sobrantes().get(i);
                var b = pieza.sobrantes().get(j);
                boolean separados = a.x().add(a.ancho()).compareTo(b.x()) <= 0
                        || b.x().add(b.ancho()).compareTo(a.x()) <= 0
                        || a.y().add(a.alto()).compareTo(b.y()) <= 0
                        || b.y().add(b.alto()).compareTo(a.y()) <= 0;
                assertTrue(separados, "Las regiones libres no deben superponerse");
            }
        }
        BigDecimal libre = pieza.sobrantes().stream()
                .map(s -> s.ancho().multiply(s.alto())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal usado = pieza.cortes().stream()
                .map(c -> c.ancho().multiply(c.alto())).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, libre.add(usado).compareTo(completa.getLargoBase()
                .multiply(bd("1000")).multiply(completa.getAnchoBase().multiply(bd("1000")))));
    }

    @Test
    void alternativasNoRepitenContenidoComercial() {
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(
                corte("1200", "900", 2),
                corte("300", "300", 2)));
        List<CalculoMedidasRespuestaDto.CombinacionDto> opciones = new java.util.ArrayList<>();
        opciones.add(respuesta.combinacionRecomendada());
        opciones.addAll(respuesta.alternativas());

        var claves = opciones.stream().map(this::claveComercial).toList();
        assertEquals(claves.size(), claves.stream().distinct().count());
    }

    @Test
    void presentacionNoConfiguradaNuncaSeUsa() {
        ProductoPrecioPresentacion invalida = presentacion(99, "Sin conversión", "UNIDAD",
                "1", "1", null, null);
        when(presentacionService.listarActivasPorProductoYSede(1, 1))
                .thenReturn(List.of(completa, invalida));

        var respuesta = calcular(cortes(corte("300", "300", 1)));

        assertFalse(ids(respuesta).contains(99));
    }

    @Test
    void superficieComercialRepresentaCantidadCobradaYContieneCortes() {
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(corte("300", "300", 1)));
        var linea = respuesta.combinacionRecomendada().lineas().get(0);
        var plano = respuesta.planoCorte().piezas().get(0);

        BigDecimal areaDibujada = plano.ancho().multiply(plano.alto())
                .divide(bd("1000000"), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal areaCobrada = linea.cantidad().multiply(linea.equivalenciaUnidadBase());
        assertTrue(areaDibujada.subtract(areaCobrada).abs().compareTo(bd("0.000001")) <= 0);
        assertEquals(List.of(1), plano.cortes().stream()
                .map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero).toList());
    }

    @Test
    void superficieComercialSeDivideAlSuperarMaximoFisicoConfigurado() {
        completa.setLargoBase(bd("1"));
        completa.setAnchoBase(bd("1"));
        var m2 = presentacion(13, "Metro cuadrado", "M2", "1",
                "0.1897929359069255442312437131", null, null);
        completa.setPrecioVenta(bd("100"));
        when(presentacionService.listarActivasPorProductoYSede(1, 1))
                .thenReturn(List.of(completa, m2));

        var respuesta = calcular(cortes(corte("900", "900", 2)));

        assertEquals("M2", respuesta.combinacionRecomendada().lineas().get(0).tipoCalculoVenta());
        assertEquals(2, respuesta.planoCorte().piezas().size());
        assertTrue(respuesta.planoCorte().piezas().stream()
                .allMatch(p -> p.ancho().compareTo(bd("1000")) <= 0
                        && p.alto().compareTo(bd("1000")) <= 0));
        BigDecimal areaPlanos = respuesta.planoCorte().piezas().stream()
                .map(p -> p.ancho().multiply(p.alto()).divide(bd("1000000"), 6,
                        java.math.RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var linea = respuesta.combinacionRecomendada().lineas().get(0);
        assertTrue(areaPlanos.subtract(linea.cantidad().multiply(linea.equivalenciaUnidadBase()))
                .abs().compareTo(bd("0.000002")) <= 0);
    }

    @Test
    void cadaCorteApareceExactamenteUnaVezEnTodasLasOpciones() {
        configurar("100", "60", "5");
        var respuesta = calcular(cortes(
                corte("1000", "700", 2),
                corte("300", "300", 2)));
        List<CalculoMedidasRespuestaDto.CombinacionDto> opciones = new java.util.ArrayList<>();
        opciones.add(respuesta.combinacionRecomendada());
        opciones.addAll(respuesta.alternativas());
        for (var opcion : opciones) {
            var base = solicitud("COTIZAR_CORTES", null, null,
                    cortes(corte("1000", "700", 2), corte("300", "300", 2)));
            var recalculada = service.calcular(conCodigo(base, opcion.codigo()), 1);
            var numeros = recalculada.planoCorte().piezas().stream().flatMap(p -> p.cortes().stream())
                    .map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero).sorted().toList();
            assertEquals(List.of(1, 2, 3, 4), numeros);
        }
    }

    private CalculoMedidasRespuestaDto calcular(List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes) {
        return service.calcular(solicitud("COTIZAR_CORTES", null, null, cortes), 1);
    }

    private CalculoMedidasSolicitudDto solicitud(String modalidad, Integer presentacion, Integer cantidad,
                                                  List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes) {
        return new CalculoMedidasSolicitudDto(1, "AREA", "MM", false, "", cortes,
                modalidad, presentacion, cantidad, null);
    }

    private String claveComercial(CalculoMedidasRespuestaDto.CombinacionDto combinacion) {
        return combinacion.lineas().stream()
                .map(l -> l.idPrecioPresentacion() + ":" + l.cantidad().stripTrailingZeros().toPlainString()
                        + ":" + l.precioUnitario().stripTrailingZeros().toPlainString()
                        + ":" + l.subtotal().stripTrailingZeros().toPlainString())
                .sorted().reduce((a, b) -> a + "|" + b).orElse("")
                + "#" + combinacion.precioTotal().stripTrailingZeros().toPlainString();
    }

    private CalculoMedidasSolicitudDto conCodigo(CalculoMedidasSolicitudDto solicitud, String codigo) {
        return new CalculoMedidasSolicitudDto(
                solicitud.idProducto(), solicitud.tipoMedida(), solicitud.unidadEntrada(),
                solicitud.entregarSobrante(), solicitud.observacion(), solicitud.cortes(),
                solicitud.modalidad(), solicitud.idPrecioPresentacion(),
                solicitud.cantidadPresentacion(), codigo);
    }

    @SafeVarargs
    private final List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes(
            CalculoMedidasSolicitudDto.CorteMedidaDto... cortes) {
        return List.of(cortes);
    }

    private CalculoMedidasSolicitudDto.CorteMedidaDto corte(String ancho, String alto, int cantidad) {
        return new CalculoMedidasSolicitudDto.CorteMedidaDto(bd(ancho), bd(alto), null, cantidad);
    }

    private List<Integer> ids(CalculoMedidasRespuestaDto respuesta) {
        return respuesta.combinacionRecomendada().lineas().stream()
                .map(CalculoMedidasRespuestaDto.LineaComercialDto::idPrecioPresentacion).toList();
    }

    private void configurar(String precioCompleta, String precioMedia, String precioPie2) {
        completa.setPrecioVenta(bd(precioCompleta));
        media.setPrecioVenta(bd(precioMedia));
        pie2.setPrecioVenta(bd(precioPie2));
        when(presentacionService.listarActivasPorProductoYSede(1, 1))
                .thenReturn(List.of(completa, media, pie2));
    }

    private ProductoPrecioPresentacion presentacion(int id, String nombre, String tipo, String precio,
                                                     String factor, String largo, String ancho) {
        ProductoPrecioPresentacion p = new ProductoPrecioPresentacion();
        p.setIdPrecioPresentacion(id);
        p.setIdProducto(1);
        p.setIdSede(1);
        p.setIdUnidadVenta(1);
        p.setEstado(1);
        p.setCantidadIncluida(BigDecimal.ONE);
        p.setNombrePresentacion(nombre);
        p.setTipoCalculoVenta(tipo);
        p.setPrecioVenta(bd(precio));
        p.setFactorConversionStock(bd(factor));
        if (largo != null) p.setLargoBase(bd(largo));
        if (ancho != null) p.setAnchoBase(bd(ancho));
        return p;
    }

    private void stock(String cantidad) {
        when(inventarioService.obtenerStockDisponibleParaVenta(1, 1)).thenReturn(bd(cantidad));
    }

    private BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
