package com.dentia.service.interno;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cálculo puro y determinista para medidas solicitadas en una venta. */
public final class CalculoCortesVenta {

    private static final BigDecimal MM2_POR_PIE2 = new BigDecimal("92903.04");
    private static final BigDecimal MM2_POR_M2 = new BigDecimal("1000000");
    private static final BigDecimal CM2_POR_PIE2 = new BigDecimal("929.0304");
    private static final BigDecimal CM2_POR_M2 = new BigDecimal("10000");
    private static final BigDecimal CM_POR_METRO = new BigDecimal("100");
    private static final BigDecimal MAX_MEDIDA = new BigDecimal("1000000");
    private static final int MAX_CORTES = 10000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CalculoCortesVenta() {
    }

    public record Resultado(BigDecimal medidaTotal, int cantidadCortes,
                            int unidadesFisicas, BigDecimal sobrante) {
    }

    private record Entrada(JsonNode cortes, String unidadEntrada) {
    }

    private record Rectangulo(BigDecimal ancho, BigDecimal alto) {
        BigDecimal area() { return ancho.multiply(alto); }
    }

    private static final class Estante {
        BigDecimal alto;
        BigDecimal usadoX;

        Estante(BigDecimal alto, BigDecimal usadoX) {
            this.alto = alto;
            this.usadoX = usadoX;
        }
    }

    private static final class Plancha {
        final List<Estante> estantes = new ArrayList<>();
    }

    /**
     * Mantiene compatibilidad con el JSON legado (arreglo en cm/m) y acepta el
     * nuevo formato operativo:
     * AREA: {"unidadEntrada":"MM","cortes":[{"ancho":500,"alto":500,"cantidad":2}]}
     * LONGITUD: {"unidadEntrada":"CM","cortes":[{"longitud":85,"cantidad":4}]}
     * Las dimensiones base deben enviarse en la misma unidad operativa del JSON.
     */
    public static Resultado calcular(String tipoMedida, String unidadVenta, String cortesJson,
                                     BigDecimal largoBase, BigDecimal anchoBase,
                                     BigDecimal medidaLongitudBase) {
        Entrada entrada = leerEntrada(cortesJson);
        String tipo = normalizar(tipoMedida);
        return switch (tipo) {
            case "AREA" -> calcularArea(entrada, unidadVenta, largoBase, anchoBase);
            case "LONGITUD" -> calcularLongitud(entrada, medidaLongitudBase);
            default -> throw new IllegalArgumentException("El producto no admite medidas de área o longitud.");
        };
    }

    private static Entrada leerEntrada(String cortesJson) {
        if (cortesJson == null || cortesJson.isBlank()) {
            throw new IllegalArgumentException("Debe registrar al menos una medida solicitada.");
        }
        final JsonNode raiz;
        try {
            raiz = MAPPER.readTree(cortesJson);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Las medidas solicitadas no tienen un formato válido.");
        }

        if (raiz.isArray()) {
            if (raiz.isEmpty()) {
                throw new IllegalArgumentException("Debe registrar al menos una medida solicitada.");
            }
            return new Entrada(raiz, "LEGACY");
        }

        if (!raiz.isObject()) {
            throw new IllegalArgumentException("Las medidas solicitadas no tienen un formato válido.");
        }
        JsonNode cortes = raiz.get("cortes");
        if (cortes == null || !cortes.isArray() || cortes.isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos una medida solicitada.");
        }
        String unidad = raiz.path("unidadEntrada").asText("");
        return new Entrada(cortes, normalizar(unidad));
    }

    private static Resultado calcularArea(Entrada entrada, String unidadVenta,
                                          BigDecimal largoBase, BigDecimal anchoBase) {
        validarBase(largoBase, "largo");
        validarBase(anchoBase, "ancho");

        boolean milimetros = "MM".equals(entrada.unidadEntrada());
        List<Rectangulo> piezas = new ArrayList<>();
        BigDecimal areaOperativa = BigDecimal.ZERO;
        for (JsonNode corte : entrada.cortes()) {
            BigDecimal ancho = milimetros
                    ? decimalAlternativo(corte, "ancho", "anchoMm")
                    : decimalAlternativo(corte, "anchoCm", "ancho");
            BigDecimal alto = milimetros
                    ? decimalAlternativo(corte, "alto", "altoMm")
                    : decimalAlternativo(corte, "altoCm", "alto");
            int cantidad = entero(corte, "cantidad");
            boolean cabeNormal = ancho.compareTo(largoBase) <= 0 && alto.compareTo(anchoBase) <= 0;
            boolean cabeGirado = alto.compareTo(largoBase) <= 0 && ancho.compareTo(anchoBase) <= 0;
            if (!cabeNormal && !cabeGirado) {
                String unidad = milimetros ? "mm" : "cm";
                throw new IllegalArgumentException("El corte " + mostrar(ancho) + " x " + mostrar(alto)
                        + " " + unidad + " no cabe en la pieza física " + mostrar(largoBase)
                        + " x " + mostrar(anchoBase) + " " + unidad + ".");
            }
            for (int i = 0; i < cantidad; i++) {
                piezas.add(new Rectangulo(ancho, alto));
            }
            areaOperativa = areaOperativa.add(ancho.multiply(alto).multiply(BigDecimal.valueOf(cantidad)));
            validarLimite(piezas.size());
        }

        piezas.sort(Comparator.comparing(Rectangulo::area).reversed()
                .thenComparing(Rectangulo::ancho, Comparator.reverseOrder())
                .thenComparing(Rectangulo::alto, Comparator.reverseOrder()));

        List<Plancha> planchas = new ArrayList<>();
        for (Rectangulo pieza : piezas) {
            boolean colocada = false;
            for (Plancha plancha : planchas) {
                if (colocar(plancha, pieza, largoBase, anchoBase)) {
                    colocada = true;
                    break;
                }
            }
            if (!colocada) {
                Plancha nueva = new Plancha();
                if (!colocar(nueva, pieza, largoBase, anchoBase)) {
                    throw new IllegalArgumentException("No fue posible distribuir los cortes en la medida base.");
                }
                planchas.add(nueva);
            }
        }

        BigDecimal capacidad = largoBase.multiply(anchoBase).multiply(BigDecimal.valueOf(planchas.size()));
        BigDecimal medida = convertirArea(areaOperativa, unidadVenta, milimetros);
        BigDecimal sobrante = convertirArea(capacidad.subtract(areaOperativa).max(BigDecimal.ZERO),
                unidadVenta, milimetros);
        return new Resultado(medida, piezas.size(), planchas.size(), sobrante);
    }

    private static boolean colocar(Plancha plancha, Rectangulo original,
                                   BigDecimal anchoPlancha, BigDecimal altoPlancha) {
        Rectangulo[] opciones = original.ancho.compareTo(original.alto) == 0
                ? new Rectangulo[]{original}
                : new Rectangulo[]{original, new Rectangulo(original.alto, original.ancho)};
        for (Rectangulo pieza : opciones) {
            for (Estante estante : plancha.estantes) {
                if (pieza.alto.compareTo(estante.alto) <= 0
                        && estante.usadoX.add(pieza.ancho).compareTo(anchoPlancha) <= 0) {
                    estante.usadoX = estante.usadoX.add(pieza.ancho);
                    return true;
                }
            }
            BigDecimal y = plancha.estantes.stream()
                    .map(e -> e.alto).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (pieza.ancho.compareTo(anchoPlancha) <= 0
                    && y.add(pieza.alto).compareTo(altoPlancha) <= 0) {
                plancha.estantes.add(new Estante(pieza.alto, pieza.ancho));
                return true;
            }
        }
        return false;
    }

    private static Resultado calcularLongitud(Entrada entrada, BigDecimal medidaBase) {
        validarBase(medidaBase, "longitud base");
        boolean centimetros = "CM".equals(entrada.unidadEntrada());
        List<BigDecimal> cortes = new ArrayList<>();
        BigDecimal totalOperativo = BigDecimal.ZERO;
        for (JsonNode corte : entrada.cortes()) {
            BigDecimal longitud = centimetros
                    ? decimalAlternativo(corte, "longitud", "longitudCm")
                    : decimalAlternativo(corte, "longitudM", "longitud");
            int cantidad = entero(corte, "cantidad");
            if (longitud.compareTo(medidaBase) > 0) {
                String unidad = centimetros ? "cm" : "m";
                throw new IllegalArgumentException("El corte de " + mostrar(longitud) + " " + unidad
                        + " no cabe en la pieza física de " + mostrar(medidaBase) + " " + unidad + ".");
            }
            for (int i = 0; i < cantidad; i++) {
                cortes.add(longitud);
            }
            totalOperativo = totalOperativo.add(longitud.multiply(BigDecimal.valueOf(cantidad)));
            validarLimite(cortes.size());
        }
        cortes.sort(Comparator.reverseOrder());
        List<BigDecimal> usados = new ArrayList<>();
        for (BigDecimal corte : cortes) {
            int destino = -1;
            for (int i = 0; i < usados.size(); i++) {
                if (usados.get(i).add(corte).compareTo(medidaBase) <= 0) {
                    destino = i;
                    break;
                }
            }
            if (destino < 0) {
                usados.add(corte);
            } else {
                usados.set(destino, usados.get(destino).add(corte));
            }
        }
        BigDecimal capacidad = medidaBase.multiply(BigDecimal.valueOf(usados.size()));
        BigDecimal totalComercial = centimetros
                ? totalOperativo.divide(CM_POR_METRO, 6, RoundingMode.HALF_UP)
                : totalOperativo.setScale(6, RoundingMode.HALF_UP);
        BigDecimal sobranteComercial = centimetros
                ? capacidad.subtract(totalOperativo).max(BigDecimal.ZERO)
                .divide(CM_POR_METRO, 6, RoundingMode.HALF_UP)
                : capacidad.subtract(totalOperativo).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
        return new Resultado(totalComercial, cortes.size(), usados.size(), sobranteComercial);
    }

    private static BigDecimal convertirArea(BigDecimal area, String unidadVenta, boolean milimetros) {
        String unidad = normalizar(unidadVenta);
        BigDecimal divisor;
        if (milimetros) {
            divisor = unidad.contains("PIE") || unidad.contains("FT") ? MM2_POR_PIE2 : MM2_POR_M2;
        } else {
            divisor = unidad.contains("PIE") || unidad.contains("FT") ? CM2_POR_PIE2 : CM2_POR_M2;
        }
        return area.divide(divisor, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimalAlternativo(JsonNode nodo, String principal, String alternativo) {
        JsonNode valor = nodo.get(principal);
        String campo = principal;
        if (valor == null || !valor.isNumber()) {
            valor = nodo.get(alternativo);
            campo = alternativo;
        }
        if (valor == null || !valor.isNumber()) {
            throw new IllegalArgumentException("Falta la medida " + principal + ".");
        }
        BigDecimal numero = valor.decimalValue();
        if (numero.compareTo(BigDecimal.ZERO) <= 0 || numero.compareTo(MAX_MEDIDA) > 0 || numero.scale() > 4) {
            throw new IllegalArgumentException("La medida " + campo
                    + " debe ser positiva y tener hasta 4 decimales.");
        }
        return numero;
    }

    private static int entero(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.canConvertToInt() || !valor.isIntegralNumber() || valor.intValue() <= 0) {
            throw new IllegalArgumentException("La cantidad de cortes debe ser un entero mayor que cero.");
        }
        return valor.intValue();
    }

    private static void validarBase(BigDecimal valor, String nombre) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El producto no tiene " + nombre + " configurado.");
        }
    }

    private static void validarLimite(int cantidad) {
        if (cantidad > MAX_CORTES) {
            throw new IllegalArgumentException("La solicitud excede el máximo de cortes permitido.");
        }
    }

    private static String mostrar(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }
}
