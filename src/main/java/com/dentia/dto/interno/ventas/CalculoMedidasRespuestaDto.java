package com.dentia.dto.interno.ventas;

import java.math.BigDecimal;
import java.util.List;

public record CalculoMedidasRespuestaDto(
        boolean ok,
        String tipoMedida,
        String unidadEntrada,
        ConsumoFisicoDto consumoFisico,
        PlanoCorteDto planoCorte,
        CombinacionDto combinacionRecomendada,
        List<CombinacionDto> alternativas,
        List<String> advertencias,
        List<String> errores
) {

    public CalculoMedidasRespuestaDto(
            boolean ok,
            String tipoMedida,
            String unidadEntrada,
            ConsumoFisicoDto consumoFisico,
            CombinacionDto combinacionRecomendada,
            List<CombinacionDto> alternativas,
            List<String> advertencias,
            List<String> errores
    ) {
        this(
                ok,
                tipoMedida,
                unidadEntrada,
                consumoFisico,
                null,
                combinacionRecomendada,
                alternativas,
                advertencias,
                errores
        );
    }

    public record ConsumoFisicoDto(
            int unidadesFisicas,
            int cantidadCortes,
            BigDecimal medidaTotal,
            BigDecimal sobrante,
            BigDecimal aprovechamientoPorcentaje
    ) {
    }

    public record PlanoCorteDto(
            String tipoMedida,
            String unidad,
            List<PiezaFisicaDto> piezas
    ) {
    }

    public record PiezaFisicaDto(
            int numero,
            String codigo,
            Integer idPrecioPresentacion,
            String nombrePresentacion,

            /*
             * Parte física concreta del inventario utilizada por este plano.
             * Permanecen en null mientras el cálculo todavía no haya asignado
             * una pieza real de inventario.
             */
            Integer idParteInventario,
            String tipoParteInventario,

            /*
             * FISICO:
             * plancha completa, media plancha, varilla, etc.
             *
             * COMERCIAL_MEDIDA:
             * pie², m² o metro lineal vendido como medida.
             */
            String tipoPlano,

            BigDecimal ancho,
            BigDecimal alto,
            BigDecimal largo,

            /*
             * Cantidad que se cobrará en la línea comercial.
             * Ejemplo: 1 plancha, 1 media plancha o 3 pie².
             */
            BigDecimal cantidadComercial,

            /*
             * Ejemplo: UNIDAD, MEDIA, PIE2, M2, METRO_LINEAL.
             */
            String unidadComercial,

            /*
             * Bloque comercial pagado dentro de la pieza física real.
             *
             * Para PIE2/M2/METRO_LINEAL:
             * representa únicamente la zona comercial cobrada al cliente.
             *
             * Para MEDIA/COMPLETA:
             * normalmente coincide con toda la pieza física seleccionada.
             */
            BloqueComercialDto bloqueComercial,

            List<CorteUbicadoDto> cortes,
            List<SobranteDto> sobrantes,
            BigDecimal medidaUtilizada,
            BigDecimal medidaSobrante,
            BigDecimal aprovechamientoPorcentaje
    ) {

        /*
         * Constructor compatible con la firma completa anterior.
         * Mantiene compilando el servicio mientras se incorpora
         * bloqueComercial en la siguiente etapa.
         */
        public PiezaFisicaDto(
                int numero,
                String codigo,
                Integer idPrecioPresentacion,
                String nombrePresentacion,
                Integer idParteInventario,
                String tipoParteInventario,
                String tipoPlano,
                BigDecimal ancho,
                BigDecimal alto,
                BigDecimal largo,
                BigDecimal cantidadComercial,
                String unidadComercial,
                List<CorteUbicadoDto> cortes,
                List<SobranteDto> sobrantes,
                BigDecimal medidaUtilizada,
                BigDecimal medidaSobrante,
                BigDecimal aprovechamientoPorcentaje
        ) {
            this(
                    numero,
                    codigo,
                    idPrecioPresentacion,
                    nombrePresentacion,
                    idParteInventario,
                    tipoParteInventario,
                    tipoPlano,
                    ancho,
                    alto,
                    largo,
                    cantidadComercial,
                    unidadComercial,
                    null,
                    cortes,
                    sobrantes,
                    medidaUtilizada,
                    medidaSobrante,
                    aprovechamientoPorcentaje
            );
        }

        /*
         * Constructor compatible con las llamadas que ya construyen
         * el plano comercial completo, antes de asociarlo a una parte
         * física concreta del inventario.
         */
        public PiezaFisicaDto(
                int numero,
                String codigo,
                Integer idPrecioPresentacion,
                String nombrePresentacion,
                String tipoPlano,
                BigDecimal ancho,
                BigDecimal alto,
                BigDecimal largo,
                BigDecimal cantidadComercial,
                String unidadComercial,
                List<CorteUbicadoDto> cortes,
                List<SobranteDto> sobrantes,
                BigDecimal medidaUtilizada,
                BigDecimal medidaSobrante,
                BigDecimal aprovechamientoPorcentaje
        ) {
            this(
                    numero,
                    codigo,
                    idPrecioPresentacion,
                    nombrePresentacion,
                    null,
                    null,
                    tipoPlano,
                    ancho,
                    alto,
                    largo,
                    cantidadComercial,
                    unidadComercial,
                    null,
                    cortes,
                    sobrantes,
                    medidaUtilizada,
                    medidaSobrante,
                    aprovechamientoPorcentaje
            );
        }

        public PiezaFisicaDto(
                int numero,
                String codigo,
                Integer idPrecioPresentacion,
                String nombrePresentacion,
                Integer idParteInventario,
                String tipoParteInventario,
                BigDecimal ancho,
                BigDecimal alto,
                BigDecimal largo,
                List<CorteUbicadoDto> cortes,
                List<SobranteDto> sobrantes,
                BigDecimal medidaUtilizada,
                BigDecimal medidaSobrante,
                BigDecimal aprovechamientoPorcentaje
        ) {
            this(numero, codigo, idPrecioPresentacion, nombrePresentacion,
                    idParteInventario, tipoParteInventario, "FISICO",
                    ancho, alto, largo, BigDecimal.ONE, null,
                    null, cortes, sobrantes, medidaUtilizada, medidaSobrante,
                    aprovechamientoPorcentaje);
        }

        /*
         * Constructor compatible con el servicio actual.
         * Permite que los planos físicos existentes sigan compilando
         * mientras agregamos la nueva lógica comercial.
         */
        public PiezaFisicaDto(
                int numero,
                String codigo,
                Integer idPrecioPresentacion,
                String nombrePresentacion,
                BigDecimal ancho,
                BigDecimal alto,
                BigDecimal largo,
                List<CorteUbicadoDto> cortes,
                List<SobranteDto> sobrantes,
                BigDecimal medidaUtilizada,
                BigDecimal medidaSobrante,
                BigDecimal aprovechamientoPorcentaje
        ) {
            this(
                    numero,
                    codigo,
                    idPrecioPresentacion,
                    nombrePresentacion,
                    null,
                    null,
                    "FISICO",
                    ancho,
                    alto,
                    largo,
                    BigDecimal.ONE,
                    null,
                    null,
                    cortes,
                    sobrantes,
                    medidaUtilizada,
                    medidaSobrante,
                    aprovechamientoPorcentaje
            );
        }
    }

    public record BloqueComercialDto(
            /*
             * Tipo de bloque:
             * MEDIDA, MEDIA, COMPLETA u otra presentación comercial.
             */
            String tipoBloque,

            /*
             * Posición del bloque dentro de la pieza física real.
             * Para área se usan x, y, ancho y alto.
             * Para longitud se usan x y largo.
             */
            BigDecimal x,
            BigDecimal y,
            BigDecimal ancho,
            BigDecimal alto,
            BigDecimal largo,

            /*
             * Cantidad cobrada y unidad comercial correspondiente.
             * Ejemplo: 17 PIE2, 1 MEDIA o 1 COMPLETA.
             */
            BigDecimal cantidadComercial,
            String unidadComercial,

            /*
             * Cortes y sobrantes que pertenecen exclusivamente
             * al bloque pagado por el cliente.
             */
            List<CorteUbicadoDto> cortes,
            List<SobranteDto> sobrantes,

            BigDecimal medidaUtilizada,
            BigDecimal medidaSobrante,
            BigDecimal aprovechamientoPorcentaje
    ) {
    }

    public record CorteUbicadoDto(
            int numero,
            String codigoGrupo,
            BigDecimal x,
            BigDecimal y,
            BigDecimal ancho,
            BigDecimal alto,
            BigDecimal largo,
            boolean rotado,
            String etiqueta
    ) {
    }

    public record SobranteDto(
            int numero,
            BigDecimal x,
            BigDecimal y,
            BigDecimal ancho,
            BigDecimal alto,
            BigDecimal largo,
            BigDecimal medida,
            boolean entregarCliente,
            String clasificacion
    ) {
    }

    public record CombinacionDto(
            String codigo,
            String motivo,
            BigDecimal precioTotal,
            BigDecimal materialAdquirido,
            int piezasFisicas,
            List<LineaComercialDto> lineas
    ) {
    }

    public record LineaComercialDto(
            Integer idPrecioPresentacion,
            String nombrePresentacion,
            String tipoCalculoVenta,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            BigDecimal factorConversionStock,
            boolean lineaPrincipal,

            /*
             * Números de los cortes asignados exclusivamente
             * a esta presentación comercial.
             *
             * Ejemplo:
             * Plancha completa → [1, 2, 3]
             * Pie² → [4]
             */
            List<Integer> numerosCorte,

            /* Equivalencia configurada de una unidad comercial en m² o m. */
            BigDecimal equivalenciaUnidadBase
    ) {

        public LineaComercialDto(
                Integer idPrecioPresentacion,
                String nombrePresentacion,
                String tipoCalculoVenta,
                BigDecimal cantidad,
                BigDecimal precioUnitario,
                BigDecimal subtotal,
                BigDecimal factorConversionStock,
                boolean lineaPrincipal,
                List<Integer> numerosCorte
        ) {
            this(idPrecioPresentacion, nombrePresentacion, tipoCalculoVenta, cantidad,
                    precioUnitario, subtotal, factorConversionStock, lineaPrincipal,
                    numerosCorte, null);
        }

        /*
         * Constructor compatible con el código actual.
         */
        public LineaComercialDto(
                Integer idPrecioPresentacion,
                String nombrePresentacion,
                String tipoCalculoVenta,
                BigDecimal cantidad,
                BigDecimal precioUnitario,
                BigDecimal subtotal,
                BigDecimal factorConversionStock,
                boolean lineaPrincipal
        ) {
            this(
                    idPrecioPresentacion,
                    nombrePresentacion,
                    tipoCalculoVenta,
                    cantidad,
                    precioUnitario,
                    subtotal,
                    factorConversionStock,
                    lineaPrincipal,
                    List.of(),
                    null
            );
        }
    }
}
