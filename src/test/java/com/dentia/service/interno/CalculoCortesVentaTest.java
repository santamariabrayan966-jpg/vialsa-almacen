package com.dentia.service.interno;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculoCortesVentaTest {

    @Test
    void calculaVariosCortesIgualesEnUnaPlancha() {
        var resultado = CalculoCortesVenta.calcular("AREA", "m2",
                "[{\"anchoCm\":100,\"altoCm\":50,\"cantidad\":4}]",
                bd("200"), bd("100"), null);

        assertEquals(4, resultado.cantidadCortes());
        assertEquals(1, resultado.unidadesFisicas());
        assertEquals(0, resultado.medidaTotal().compareTo(bd("2.000000")));
        assertEquals(0, resultado.sobrante().compareTo(bd("0.000000")));
    }

    @Test
    void distribuyeMedidasDiferentesEnMasDeUnaPlancha() {
        var resultado = CalculoCortesVenta.calcular("AREA", "m2",
                "[{\"anchoCm\":150,\"altoCm\":80,\"cantidad\":2},"
                        + "{\"anchoCm\":100,\"altoCm\":100,\"cantidad\":1}]",
                bd("200"), bd("100"), null);

        assertEquals(3, resultado.cantidadCortes());
        assertEquals(3, resultado.unidadesFisicas());
        assertEquals(0, resultado.medidaTotal().compareTo(bd("3.400000")));
    }

    @Test
    void rechazaCorteQueNoCabeAunqueSuAreaSeaMenor() {
        assertThrows(IllegalArgumentException.class, () ->
                CalculoCortesVenta.calcular("AREA", "m2",
                        "[{\"anchoCm\":210,\"altoCm\":40,\"cantidad\":1}]",
                        bd("200"), bd("100"), null));
    }

    @Test
    void permiteRotarCorteCuandoAsiCabe() {
        var resultado = CalculoCortesVenta.calcular("AREA", "m2",
                "[{\"anchoCm\":90,\"altoCm\":180,\"cantidad\":1}]",
                bd("200"), bd("100"), null);

        assertEquals(1, resultado.unidadesFisicas());
    }

    @Test
    void distribuyeLongitudesEnVariasVarillas() {
        var resultado = CalculoCortesVenta.calcular("LONGITUD", "m",
                "[{\"longitudM\":4,\"cantidad\":2},{\"longitudM\":2,\"cantidad\":2}]",
                null, null, bd("6"));

        assertEquals(4, resultado.cantidadCortes());
        assertEquals(2, resultado.unidadesFisicas());
        assertEquals(0, resultado.medidaTotal().compareTo(bd("12.000000")));
        assertEquals(0, resultado.sobrante().compareTo(bd("0.000000")));
    }

    @Test
    void rechazaCantidadFraccionariaOVacia() {
        assertThrows(IllegalArgumentException.class, () ->
                CalculoCortesVenta.calcular("LONGITUD", "m",
                        "[{\"longitudM\":2,\"cantidad\":1.5}]",
                        null, null, bd("6")));
    }


    @Test
    void calculaAreaOperativaEnMilimetros() {
        var resultado = CalculoCortesVenta.calcular("AREA", "PIE2",
                "{\"tipoMedida\":\"AREA\",\"unidadEntrada\":\"MM\","
                        + "\"cortes\":[{\"ancho\":500,\"alto\":500,\"cantidad\":4}]}",
                bd("1000"), bd("1000"), null);

        assertEquals(4, resultado.cantidadCortes());
        assertEquals(1, resultado.unidadesFisicas());
        assertEquals(0, resultado.medidaTotal().compareTo(bd("10.763910")));
    }

    @Test
    void calculaLongitudOperativaEnCentimetrosYDevuelveMetros() {
        var resultado = CalculoCortesVenta.calcular("LONGITUD", "METRO_LINEAL",
                "{\"tipoMedida\":\"LONGITUD\",\"unidadEntrada\":\"CM\","
                        + "\"cortes\":[{\"longitud\":85,\"cantidad\":4},"
                        + "{\"longitud\":120,\"cantidad\":2}]}",
                null, null, bd("600"));

        assertEquals(6, resultado.cantidadCortes());
        assertEquals(1, resultado.unidadesFisicas());
        assertEquals(0, resultado.medidaTotal().compareTo(bd("5.800000")));
        assertEquals(0, resultado.sobrante().compareTo(bd("0.200000")));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
