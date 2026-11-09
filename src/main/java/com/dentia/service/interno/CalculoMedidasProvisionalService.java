package com.dentia.service.interno;

import com.dentia.dto.interno.ventas.CalculoMedidasRespuestaDto;
import com.dentia.dto.interno.ventas.CalculoMedidasSolicitudDto;
import com.dentia.model.interno.Producto;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import com.dentia.model.interno.inventario.InventarioParte;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CalculoMedidasProvisionalService {
    private static final int MAX_CANDIDATAS = 25000;
    private static final int MAX_BASES_HIBRIDAS = 500;
    private final ProductoService productoService;
    private final ProductoPrecioPresentacionService presentacionService;
    private final InventarioService inventarioService;

    public CalculoMedidasProvisionalService(ProductoService productoService,
                                            ProductoPrecioPresentacionService presentacionService,
                                            InventarioService inventarioService) {
        this.productoService = productoService;
        this.presentacionService = presentacionService;
        this.inventarioService = inventarioService;
    }

    public CalculoMedidasRespuestaDto calcular(CalculoMedidasSolicitudDto solicitud, Integer idSede) {
        validarSolicitud(solicitud, idSede);
        Producto producto = productoService.obtener(solicitud.idProducto());
        if (producto == null || producto.getIdEstadoProducto() == null || producto.getIdEstadoProducto() != 1) {
            throw new IllegalArgumentException("El producto no existe o no está activo.");
        }
        if (!Boolean.TRUE.equals(producto.getVentaPorMedida())) {
            throw new IllegalArgumentException("El producto seleccionado no admite venta por medidas.");
        }
        String tipo = normalizar(producto.getTipoMedidaBase());
        if (!tipo.equals("AREA") && !tipo.equals("LONGITUD")) {
            throw new IllegalArgumentException("El producto no tiene un tipo de medida válido.");
        }
        String unidadEntrada = tipo.equals("AREA") ? "MM" : "CM";
        validarUnidadYTipo(solicitud, tipo, unidadEntrada);

        List<Corte> cortes = expandirCortes(solicitud, tipo);
        BigDecimal medidaTotal = calcularMedidaTotal(cortes, tipo);
        int cantidadCortes = cortes.size();

        List<ProductoPrecioPresentacion> activas = presentacionService
                .listarActivasPorProductoYSede(producto.getIdProducto(), idSede).stream()
                .filter(p -> p != null
                        && Objects.equals(p.getIdProducto(), producto.getIdProducto())
                        && p.getIdPrecioPresentacion() != null
                        && p.getIdPrecioPresentacion() > 0
                        && positivo(p.getPrecioVenta())
                        && positivo(p.getFactorConversionStock())
                        && !normalizar(p.getTipoCalculoVenta()).isBlank())
                .toList();
        if (activas.isEmpty()) {
            throw new IllegalArgumentException("El producto no tiene presentaciones activas con precio en la sede.");
        }

        BigDecimal stockConsultado = inventarioService
                .obtenerStockDisponibleParaVenta(producto.getIdProducto(), idSede);
        BigDecimal stockDisponible = stockConsultado == null ? BigDecimal.ZERO : stockConsultado.max(BigDecimal.ZERO);

        InventarioService.DisponibilidadFisicaProducto disponibilidadFisica =
                inventarioService.obtenerDisponibilidadFisicaProducto(
                        producto.getIdProducto(),
                        idSede
                );

        BigDecimal stockFisicoDisponible =
                calcularStockFisicoEquivalente(
                        disponibilidadFisica,
                        stockDisponible
                );

        List<String> advertencias = new ArrayList<>();
        List<Fisica> fisicas = construirFisicas(activas, producto, tipo, unidadEntrada, cortes, advertencias);
        List<ProductoPrecioPresentacion> sueltas = activas.stream()
                .filter(this::esSuelta)
                .filter(p -> esSueltaCompatible(p, tipo))
                .toList();
        if (fisicas.isEmpty() && sueltas.isEmpty()) {
            throw new IllegalArgumentException("No existe una presentación válida y suficientemente configurada para realizar los cortes.");
        }

        String modalidad = normalizar(solicitud.modalidad());
        if (modalidad.isBlank()) modalidad = "COTIZAR_CORTES";

        List<CalculoMedidasRespuestaDto.CombinacionDto> candidatas;
        if (modalidad.equals("COMPRAR_PRESENTACION")) {
            candidatas = new ArrayList<>(List.of(generarPresentacionFija(
                    solicitud,
                    fisicas,
                    sueltas,
                    cortes,
                    tipo,
                    stockFisicoDisponible,
                    disponibilidadFisica,
                    producto
            )));
        } else if (modalidad.equals("COTIZAR_CORTES")) {
            candidatas = generarCombinaciones(
                    fisicas,
                    sueltas,
                    cortes,
                    tipo,
                    medidaTotal,
                    stockFisicoDisponible,
                    disponibilidadFisica,
                    advertencias,
                    producto
            );
        } else {
            throw new IllegalArgumentException("La modalidad de venta por medidas no es válida.");
        }
        if (candidatas.isEmpty()) {
            throw new IllegalArgumentException("No existe una combinación físicamente válida y con stock suficiente para las medidas solicitadas.");
        }

        candidatas.sort(comparador());
        CalculoMedidasRespuestaDto.CombinacionDto recomendada = candidatas.get(0);
        if (solicitud.codigoCombinacion() != null && !solicitud.codigoCombinacion().isBlank()) {
            String codigoElegido = solicitud.codigoCombinacion().trim();
            recomendada = candidatas.stream()
                    .filter(c -> Objects.equals(c.codigo(), codigoElegido))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La opción comercial elegida ya no está disponible. Vuelva a calcular."));
        }
        BigDecimal capacidad = recomendada.materialAdquirido();
        BigDecimal usadoOperativo = tipo.equals("AREA")
                ? medidaTotal.multiply(new BigDecimal("1000000"))
                : medidaTotal.multiply(new BigDecimal("100"));
        BigDecimal capacidadOperativa = tipo.equals("AREA")
                ? capacidad.multiply(new BigDecimal("1000000"))
                : capacidad.multiply(new BigDecimal("100"));
        BigDecimal sobrante = capacidad.subtract(medidaTotal).max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
        BigDecimal aprovechamiento = capacidadOperativa.signum() == 0 ? BigDecimal.ZERO
                : usadoOperativo.multiply(BigDecimal.valueOf(100)).divide(capacidadOperativa, 2, RoundingMode.HALF_UP);

        var consumo = new CalculoMedidasRespuestaDto.ConsumoFisicoDto(
                recomendada.piezasFisicas(),
                cantidadCortes,
                medidaTotal.setScale(6, RoundingMode.HALF_UP),
                sobrante,
                aprovechamiento
        );

        var planoCorte = construirPlanoCorte(
                recomendada,
                candidatas,
                fisicas,
                cortes,
                tipo,
                unidadEntrada,
                Boolean.TRUE.equals(solicitud.entregarSobrante()),
                disponibilidadFisica
        );
        validarAsignacionUnica(planoCorte, cortes.size());
        recomendada = asignarCortesALineas(recomendada, planoCorte);
        consumo = new CalculoMedidasRespuestaDto.ConsumoFisicoDto(
                planoCorte.piezas().size(), cantidadCortes,
                medidaTotal.setScale(6, RoundingMode.HALF_UP), sobrante, aprovechamiento);

        List<CalculoMedidasRespuestaDto.CombinacionDto> alternativas = new ArrayList<>();
        for (CalculoMedidasRespuestaDto.CombinacionDto candidata : candidatas) {
            if (Objects.equals(candidata.codigo(), recomendada.codigo())) continue;
            if (alternativas.size() >= 5) break;
            try {
                CalculoMedidasRespuestaDto.PlanoCorteDto planoAlternativo = construirPlanoCorte(
                        candidata, candidatas, fisicas, cortes, tipo, unidadEntrada,
                        Boolean.TRUE.equals(solicitud.entregarSobrante()),
                        disponibilidadFisica);
                validarAsignacionUnica(planoAlternativo, cortes.size());
                alternativas.add(asignarCortesALineas(candidata, planoAlternativo));
            } catch (IllegalArgumentException ex) {
                advertencias.add("Se omitió una alternativa cuyo plano no pudo reconstruirse: " + ex.getMessage());
            }
        }

        return new CalculoMedidasRespuestaDto(
                true,
                tipo,
                unidadEntrada,
                consumo,
                planoCorte,
                recomendada,
                alternativas,
                advertencias,
                List.of()
        );
    }

    private void validarAsignacionUnica(CalculoMedidasRespuestaDto.PlanoCorteDto plano, int cantidadCortes) {
        int[] apariciones = new int[cantidadCortes + 1];
        if (plano == null || plano.piezas() == null) {
            throw new IllegalArgumentException("El cálculo no generó planos para los cortes solicitados.");
        }
        for (CalculoMedidasRespuestaDto.PiezaFisicaDto pieza : plano.piezas()) {
            if (pieza.cortes() == null) continue;
            for (CalculoMedidasRespuestaDto.CorteUbicadoDto corte : pieza.cortes()) {
                if (corte.numero() <= 0 || corte.numero() > cantidadCortes) {
                    throw new IllegalArgumentException("El plano contiene una numeración de corte inválida.");
                }
                apariciones[corte.numero()]++;
            }
        }
        for (int numero = 1; numero <= cantidadCortes; numero++) {
            if (apariciones[numero] != 1) {
                throw new IllegalArgumentException("El corte " + numero
                        + " debe aparecer exactamente una vez en la combinación.");
            }
        }
    }

    private CalculoMedidasRespuestaDto.CombinacionDto generarPresentacionFija(
            CalculoMedidasSolicitudDto solicitud,
            List<Fisica> fisicas,
            List<ProductoPrecioPresentacion> sueltas,
            List<Corte> cortes,
            String tipo,
            BigDecimal stockDisponible,
            InventarioService.DisponibilidadFisicaProducto disponibilidadFisica,
            Producto producto
    ) {
        if (solicitud.idPrecioPresentacion() == null || solicitud.idPrecioPresentacion() <= 0) {
            throw new IllegalArgumentException("Seleccione una presentación válida para realizar los cortes.");
        }


        Fisica fisica = fisicas.stream()
                .filter(f -> Objects.equals(f.presentacion.getIdPrecioPresentacion(),
                        solicitud.idPrecioPresentacion()))
                .findFirst()
                .orElse(null);

        if (fisica == null) {
            ProductoPrecioPresentacion suelta = sueltas.stream()
                    .filter(p -> Objects.equals(p.getIdPrecioPresentacion(), solicitud.idPrecioPresentacion()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La presentación elegida no está activa o no tiene configuración suficiente."));

            validarCortesContraInventarioFisico(
                    fisicas,
                    cortes,
                    tipo,
                    disponibilidadFisica
            );

            if (disponibilidadFisica != null
                    && disponibilidadFisica.tienePartes()
                    && disponibilidadFisica.medidaVendibleTotal() <= 0) {
                throw new IllegalArgumentException(
                        "No existe material físico disponible para vender esta presentación por medida."
                );
            }
            BigDecimal medidaTotal = calcularMedidaTotal(cortes, tipo);
            BigDecimal equivalencia = equivalenciaUnidadBase(suelta, producto, tipo);
            BigDecimal factorUnidad = factorStockPorUnidadComercial(suelta);
            BigDecimal cantidad = cantidadSuelta(medidaTotal, suelta, equivalencia);
            BigDecimal consumoStock = factorUnidad.multiply(cantidad);
            if (consumoStock.compareTo(stockDisponible) > 0) {
                throw new IllegalArgumentException("Stock insuficiente para la presentación elegida. Disponible: "
                        + stockDisponible.stripTrailingZeros().toPlainString() + ", requerido: "
                        + consumoStock.stripTrailingZeros().toPlainString() + ".");
            }
            BigDecimal subtotal = suelta.getPrecioVenta().multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            List<Integer> numeros = java.util.stream.IntStream.rangeClosed(1, cortes.size()).boxed().toList();
            var linea = new CalculoMedidasRespuestaDto.LineaComercialDto(
                    suelta.getIdPrecioPresentacion(), suelta.getNombrePresentacion(),
                    suelta.getTipoCalculoVenta(), cantidad, suelta.getPrecioVenta(), subtotal,
                    factorUnidad, true, numeros, equivalencia);
            BigDecimal material = cantidad.multiply(equivalencia);
            return new CalculoMedidasRespuestaDto.CombinacionDto(
                    "FIJA-MEDIDA-" + suelta.getIdPrecioPresentacion(),
                    "Se cobra la cantidad comercial calculada para la presentación por medida elegida.",
                    subtotal, material.setScale(10, RoundingMode.HALF_UP), 1, List.of(linea));
        }

        for (int numero = 0; numero < cortes.size(); numero++) {
            Corte corte = cortes.get(numero);
            boolean cabe = tipo.equals("AREA")
                    ? ((corte.ancho.compareTo(fisica.largo) <= 0 && corte.alto.compareTo(fisica.ancho) <= 0)
                    || (corte.alto.compareTo(fisica.largo) <= 0 && corte.ancho.compareTo(fisica.ancho) <= 0))
                    : corte.longitud.compareTo(fisica.longitud) <= 0;
            if (!cabe) {
                throw new IllegalArgumentException("El corte " + (numero + 1) + " no cabe individualmente en "
                        + fisica.presentacion.getNombrePresentacion() + " ni con una rotación válida.");
            }
        }

        int cantidadMinima = 0;
        for (int cantidad = 1; cantidad <= 100; cantidad++) {
            List<Bin> bins = new ArrayList<>();
            for (int i = 0; i < cantidad; i++) bins.add(new Bin(fisica.largo, fisica.ancho, fisica.longitud));
            if (caben(cortes, bins, tipo)) {
                cantidadMinima = cantidad;
                break;
            }
        }
        if (cantidadMinima == 0) {
            throw new IllegalArgumentException("Los cortes requieren más de 100 unidades de "
                    + fisica.presentacion.getNombrePresentacion() + ".");
        }
        int cantidad = Math.max(cantidadMinima, solicitud.cantidadPresentacion() != null ? solicitud.cantidadPresentacion() : cantidadMinima);

        int cantidadFisicaDisponible =
                maximoDisponibleParaPresentacion(
                        fisica,
                        disponibilidadFisica,
                        stockDisponible
                );

        if (disponibilidadFisica != null && disponibilidadFisica.tienePartes() && cantidad > cantidadFisicaDisponible) {
            throw new IllegalArgumentException(
                    "Stock físico insuficiente para "
                            + fisica.presentacion.getNombrePresentacion()
                            + ". Cantidad requerida: "
                            + cantidad
                            + ", cantidad físicamente disponible: "
                            + cantidadFisicaDisponible
                            + "."
            );
        }

        BigDecimal cantidadDecimal = BigDecimal.valueOf(cantidad);
        BigDecimal consumoStock = fisica.factorStock.multiply(cantidadDecimal);
        if (consumoStock.compareTo(stockDisponible) > 0) {
            int disponibles = stockDisponible.divide(fisica.factorStock, 0, RoundingMode.FLOOR).intValue();
            throw new IllegalArgumentException("Stock insuficiente para " + fisica.presentacion.getNombrePresentacion()
                    + ". Cantidad requerida: " + cantidad + ", cantidad disponible: " + disponibles + ".");
        }

        BigDecimal subtotal = fisica.presentacion.getPrecioVenta()
                .multiply(cantidadDecimal).setScale(2, RoundingMode.HALF_UP);
        List<Integer> numeros = java.util.stream.IntStream.rangeClosed(1, cortes.size()).boxed().toList();
        var linea = new CalculoMedidasRespuestaDto.LineaComercialDto(
                fisica.presentacion.getIdPrecioPresentacion(),
                fisica.presentacion.getNombrePresentacion(),
                fisica.presentacion.getTipoCalculoVenta(),
                cantidadDecimal,
                fisica.presentacion.getPrecioVenta(),
                subtotal,
                fisica.factorStock,
                true,
                numeros
        );
        return new CalculoMedidasRespuestaDto.CombinacionDto(
                "FIJA-" + solicitud.idPrecioPresentacion() + "x" + cantidad,
                "El sistema calculó automáticamente la cantidad mínima necesaria para realizar todos los cortes.",
                subtotal,
                fisica.capacidadCobro.multiply(cantidadDecimal).setScale(10, RoundingMode.HALF_UP),
                cantidad,
                List.of(linea)
        );
    }

    private CalculoMedidasRespuestaDto.CombinacionDto asignarCortesALineas(
            CalculoMedidasRespuestaDto.CombinacionDto combinacion,
            CalculoMedidasRespuestaDto.PlanoCorteDto plano
    ) {
        Map<Integer, LinkedHashSet<Integer>> porPresentacion = new LinkedHashMap<>();
        if (plano != null && plano.piezas() != null) {
            for (CalculoMedidasRespuestaDto.PiezaFisicaDto pieza : plano.piezas()) {
                if (pieza.idPrecioPresentacion() == null || pieza.cortes() == null) continue;
                LinkedHashSet<Integer> numeros = porPresentacion.computeIfAbsent(
                        pieza.idPrecioPresentacion(), ignored -> new LinkedHashSet<>());
                pieza.cortes().stream().map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero)
                        .forEach(numeros::add);
            }
        }

        boolean soloLineaSuelta = combinacion.lineas() != null
                && combinacion.lineas().size() == 1
                && esSueltaTipo(combinacion.lineas().get(0).tipoCalculoVenta());

        LinkedHashSet<Integer> todosLosCortes = new LinkedHashSet<>();
        porPresentacion.values().forEach(todosLosCortes::addAll);

        List<CalculoMedidasRespuestaDto.LineaComercialDto> lineas = new ArrayList<>();
        for (int i = 0; i < combinacion.lineas().size(); i++) {
            var linea = combinacion.lineas().get(i);
            List<Integer> numeros = soloLineaSuelta
                    ? new ArrayList<>(todosLosCortes)
                    : new ArrayList<>(porPresentacion.getOrDefault(
                    linea.idPrecioPresentacion(), new LinkedHashSet<>()));
            lineas.add(new CalculoMedidasRespuestaDto.LineaComercialDto(
                    linea.idPrecioPresentacion(), linea.nombrePresentacion(), linea.tipoCalculoVenta(),
                    linea.cantidad(), linea.precioUnitario(), linea.subtotal(),
                    linea.factorConversionStock(), i == 0, List.copyOf(numeros),
                    linea.equivalenciaUnidadBase()));
        }
        return new CalculoMedidasRespuestaDto.CombinacionDto(
                combinacion.codigo(), combinacion.motivo(), combinacion.precioTotal(),
                combinacion.materialAdquirido(), plano == null || plano.piezas() == null
                ? combinacion.piezasFisicas() : plano.piezas().size(), List.copyOf(lineas));
    }

    private List<CalculoMedidasRespuestaDto.CombinacionDto> generarCombinaciones(
            List<Fisica> fisicas,
            List<ProductoPrecioPresentacion> sueltas,
            List<Corte> cortes,
            String tipo,
            BigDecimal medidaTotal,
            BigDecimal stockDisponible,
            InventarioService.DisponibilidadFisicaProducto disponibilidadFisica,
            List<String> advertencias,
            Producto producto
    ) {
        List<CalculoMedidasRespuestaDto.CombinacionDto> out =
                new ArrayList<>();

        int n = Math.min(fisicas.size(), 6);
        int[] max = new int[n];

        for (int i = 0; i < n; i++) {
            BigDecimal capacidad = fisicas.get(i).capacidadCobro;

            int porCapacidad = capacidad.signum() <= 0
                    ? 0
                    : medidaTotal
                    .divide(capacidad, 0, RoundingMode.CEILING)
                    .intValue() + 2;

            BigDecimal factor = fisicas.get(i).factorStock;

            int porStock = factor.signum() <= 0
                    ? 0
                    : stockDisponible
                    .divide(factor, 0, RoundingMode.FLOOR)
                    .intValue();

            int porInventarioFisico =
                    maximoDisponibleParaPresentacion(
                            fisicas.get(i),
                            disponibilidadFisica,
                            stockDisponible
                    );

            max[i] = Math.max(
                    0,
                    Math.min(
                            100,
                            Math.min(
                                    porCapacidad,
                                    Math.min(porStock, porInventarioFisico)
                            )
                    )
            );
        }

        int[] counts = new int[n];
        int[] visitadas = {0};

        enumerar(
                0,
                counts,
                max,
                fisicas.subList(0, n),
                cortes,
                tipo,
                stockDisponible,
                disponibilidadFisica,
                out,
                visitadas
        );

        /*
         * Guardamos las combinaciones físicas completas antes de agregar
         * opciones comerciales por PIE2, M2 o metro lineal.
         */
        List<CalculoMedidasRespuestaDto.CombinacionDto> combinacionesFisicas =
                new ArrayList<>(out);

        /*
         * Para productos de área, intenta reemplazar la pieza física menos
         * aprovechada por el cobro exacto en PIE2 o M2.
         *
         * La sustitución se calcula exclusivamente con las presentaciones y
         * conversiones configuradas para el producto.
         */
        if (tipo.equals("AREA")
                && !sueltas.isEmpty()
                && !combinacionesFisicas.isEmpty()) {

            List<CalculoMedidasRespuestaDto.CombinacionDto> basesHibridas =
                    combinacionesFisicas.stream()
                            .sorted(comparador())
                            .limit(MAX_BASES_HIBRIDAS)
                            .toList();
            if (combinacionesFisicas.size() > MAX_BASES_HIBRIDAS) {
                advertencias.add(
                        "La búsqueda híbrida se limitó a las " + MAX_BASES_HIBRIDAS
                                + " mejores bases físicas para mantener un tiempo de respuesta seguro."
                );
            }
            out.addAll(
                    generarCombinacionesHibridasArea(
                            basesHibridas,
                            fisicas,
                            sueltas,
                            cortes,
                            stockDisponible,
                            producto
                    )
            );
        }

        /*
         * También permite vender toda la solicitud por medida suelta,
         * siempre que su consumo proporcional no supere el stock y que
         * cada corte pueda asignarse a una pieza física real disponible.
         */
        boolean cortesCabenEnInventarioFisico =
                (disponibilidadFisica == null || !disponibilidadFisica.tienePartes())
                        || caben(
                                cortes,
                                construirBinsFisicosDisponibles(
                                        fisicas,
                                        disponibilidadFisica
                                ),
                                tipo
                        );

        if (!sueltas.isEmpty()
                && cortesCabenEnInventarioFisico) {

            CalculoMedidasRespuestaDto.CombinacionDto fuenteFisica =
                    combinacionesFisicas.stream()
                            .min(
                                    Comparator
                                            .comparing(
                                                    CalculoMedidasRespuestaDto.CombinacionDto::materialAdquirido
                                            )
                                            .thenComparingInt(
                                                    CalculoMedidasRespuestaDto.CombinacionDto::piezasFisicas
                                            )
                            )
                            .orElse(null);

            for (ProductoPrecioPresentacion suelta : sueltas) {
                if (!positivo(suelta.getFactorConversionStock())) {
                    continue;
                }

                BigDecimal equivalencia = equivalenciaUnidadBase(suelta, producto, tipo);
                BigDecimal cantidad = cantidadSuelta(medidaTotal, suelta, equivalencia);

                if (cantidad.signum() <= 0) {
                    continue;
                }

                BigDecimal factor = factorStockPorUnidadComercial(suelta);

                BigDecimal consumoStock =
                        factor.multiply(cantidad);

                if (consumoStock.compareTo(stockDisponible) > 0) {
                    continue;
                }

                BigDecimal subtotal =
                        suelta.getPrecioVenta()
                                .multiply(cantidad)
                                .setScale(2, RoundingMode.HALF_UP);

                List<CalculoMedidasRespuestaDto.LineaComercialDto> lineas =
                        List.of(
                                new CalculoMedidasRespuestaDto.LineaComercialDto(
                                        suelta.getIdPrecioPresentacion(),
                                        suelta.getNombrePresentacion(),
                                        suelta.getTipoCalculoVenta(),
                                        cantidad,
                                        suelta.getPrecioVenta(),
                                        subtotal,
                                        factor,
                                        true,
                                        List.of(),
                                        equivalencia
                                )
                        );

                out.add(
                        new CalculoMedidasRespuestaDto.CombinacionDto(
                                "SUELTA-" + suelta.getIdPrecioPresentacion(),
                                "Toda la solicitud se cobra por la medida exacta requerida.",
                                subtotal,
                                cantidad.multiply(equivalencia).setScale(
                                        10,
                                        RoundingMode.HALF_UP
                                ),
                                fuenteFisica == null ? 1 : fuenteFisica.piezasFisicas(),
                                lineas
                        )
                );
            }
        }

        if (visitadas[0] >= MAX_CANDIDATAS) {
            advertencias.add(
                    "La búsqueda alcanzó el límite de seguridad; se conservaron las mejores combinaciones encontradas."
            );
        }

        return deduplicar(out);
    }

    private List<CalculoMedidasRespuestaDto.CombinacionDto>
    generarCombinacionesHibridasArea(
            List<CalculoMedidasRespuestaDto.CombinacionDto> combinacionesFisicas,
            List<Fisica> fisicas,
            List<ProductoPrecioPresentacion> sueltas,
            List<Corte> cortes,
            BigDecimal stockDisponible,
            Producto producto
    ) {
        List<CalculoMedidasRespuestaDto.CombinacionDto> resultado =
                new ArrayList<>();

        for (CalculoMedidasRespuestaDto.CombinacionDto base
                : combinacionesFisicas) {

            List<Fisica> piezasBase =
                    expandirPiezasFisicas(base, fisicas);

            /*
             * Para crear una combinación híbrida debe existir al menos
             * una pieza física que pueda retirarse.
             */
            if (piezasBase.size() <= 1) {
                continue;
            }

            for (int indiceRetirada = 0;
                 indiceRetirada < piezasBase.size();
                 indiceRetirada++) {

                Fisica fisicaRetirada =
                        piezasBase.get(indiceRetirada);

                List<Fisica> piezasRestantes =
                        new ArrayList<>(piezasBase);

                piezasRestantes.remove(indiceRetirada);

                /*
                 * Busca la menor cantidad posible de cortes pequeños que,
                 * al separarse, permite colocar todos los demás cortes
                 * en las piezas físicas restantes.
                 */
                ResidualArea residual =
                        buscarResidualMinimoArea(
                                piezasRestantes,
                                cortes
                        );

                if (residual == null
                        || residual.numerosCorte().isEmpty()
                        || residual.areaM2().signum() <= 0) {
                    continue;
                }

                for (ProductoPrecioPresentacion suelta : sueltas) {
                    if (!positivo(suelta.getFactorConversionStock())) {
                        continue;
                    }

                    String tipoSuelta =
                            normalizar(suelta.getTipoCalculoVenta());

                    if (!tipoSuelta.equals("PIE2")
                            && !tipoSuelta.equals("M2")
                            && !tipoSuelta.equals("METRO_CUADRADO")
                            && !tipoSuelta.equals("AREA")) {
                        continue;
                    }

                    BigDecimal equivalencia = equivalenciaUnidadBase(suelta, producto, "AREA");
                    BigDecimal factorUnidad = factorStockPorUnidadComercial(suelta);
                    BigDecimal cantidadSuelta = cantidadSuelta(residual.areaM2(), suelta, equivalencia);

                    if (cantidadSuelta.signum() <= 0) {
                        continue;
                    }

                    List<CalculoMedidasRespuestaDto.LineaComercialDto> lineas =
                            crearLineasHibridasResiduales(
                                    base.lineas(),
                                    fisicaRetirada,
                                    suelta,
                                    cantidadSuelta,
                                    residual.numerosCorte(),
                                    cortes.size(),
                                    factorUnidad,
                                    equivalencia
                            );

                    if (lineas.isEmpty()) {
                        continue;
                    }

                    BigDecimal consumoStock =
                            calcularConsumoStockLineas(lineas);

                    if (consumoStock.compareTo(stockDisponible) > 0) {
                        continue;
                    }

                    BigDecimal precioTotal =
                            lineas.stream()
                                    .map(
                                            CalculoMedidasRespuestaDto
                                                    .LineaComercialDto::subtotal
                                    )
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    )
                                    .setScale(
                                            2,
                                            RoundingMode.HALF_UP
                                    );

                    BigDecimal capacidadSueltaM2 = cantidadSuelta.multiply(equivalencia);

                    BigDecimal materialAdquirido =
                            base.materialAdquirido()
                                    .subtract(
                                            fisicaRetirada.capacidadCobro
                                    )
                                    .add(capacidadSueltaM2)
                                    .max(BigDecimal.ZERO)
                                    .setScale(
                                            10,
                                            RoundingMode.HALF_UP
                                    );

                    /*
                     * Solo conserva la combinación híbrida cuando realmente
                     * mejora el precio de la combinación física original.
                     */
                    if (precioTotal.compareTo(base.precioTotal()) >= 0) {
                        continue;
                    }

                    resultado.add(
                            new CalculoMedidasRespuestaDto.CombinacionDto(
                                    "RESIDUAL|"
                                            + base.codigo()
                                            + "|"
                                            + indiceRetirada
                                            + "|"
                                            + suelta.getIdPrecioPresentacion()
                                            + "|"
                                            + residual.numerosCorte()
                                            .stream()
                                            .map(String::valueOf)
                                            .collect(
                                                    java.util.stream.Collectors
                                                            .joining("_")
                                            ),
                                    "Los cortes pequeños residuales se cobran por medida y los demás permanecen en sus presentaciones físicas.",
                                    precioTotal,
                                    materialAdquirido,

                                    /*
                                     * Piezas físicas restantes más un plano
                                     * comercial para los pie².
                                     */
                                    piezasRestantes.size() + 1,
                                    lineas
                            )
                    );
                }
            }
        }

        return resultado;
    }

    private ResidualArea buscarResidualMinimoArea(
            List<Fisica> piezasRestantes,
            List<Corte> cortes
    ) {
        List<CorteNumerado> ordenados =
                new ArrayList<>();

        for (int i = 0; i < cortes.size(); i++) {
            ordenados.add(
                    new CorteNumerado(
                            i + 1,
                            cortes.get(i)
                    )
            );
        }

        /*
         * Empieza retirando los cortes de menor área.
         */
        ordenados.sort(
                Comparator.comparing(
                        c -> c.corte.ancho
                                .multiply(c.corte.alto)
                )
        );

        List<Integer> residuales =
                new ArrayList<>();

        for (int cantidadResidual = 1;
             cantidadResidual <= ordenados.size();
             cantidadResidual++) {

            CorteNumerado nuevoResidual =
                    ordenados.get(cantidadResidual - 1);

            residuales.add(nuevoResidual.numero);

            Set<Integer> residualesSet =
                    new HashSet<>(residuales);

            List<Corte> cortesRestantes =
                    new ArrayList<>();

            for (int i = 0; i < cortes.size(); i++) {
                int numeroCorte = i + 1;

                if (!residualesSet.contains(numeroCorte)) {
                    cortesRestantes.add(cortes.get(i));
                }
            }

            List<Bin> bins =
                    piezasRestantes.stream()
                            .map(fisica ->
                                    new Bin(
                                            fisica.largo,
                                            fisica.ancho,
                                            fisica.longitud
                                    )
                            )
                            .toList();

            boolean cabenRestantes =
                    cortesRestantes.isEmpty()
                            || caben(
                            cortesRestantes,
                            bins,
                            "AREA"
                    );

            if (!cabenRestantes) {
                continue;
            }

            BigDecimal areaResidualM2 =
                    residuales.stream()
                            .map(numero ->
                                    cortes.get(numero - 1)
                            )
                            .map(corte ->
                                    corte.ancho
                                            .multiply(corte.alto)
                                            .divide(
                                                    new BigDecimal("1000000"),
                                                    10,
                                                    RoundingMode.HALF_UP
                                            )
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            return new ResidualArea(
                    List.copyOf(residuales),
                    areaResidualM2
            );
        }

        return null;
    }
    private List<CalculoMedidasRespuestaDto.LineaComercialDto>
    crearLineasHibridasResiduales(
            List<CalculoMedidasRespuestaDto.LineaComercialDto> lineasBase,
            Fisica fisicaRetirada,
            ProductoPrecioPresentacion suelta,
            BigDecimal cantidadSuelta,
            List<Integer> numerosCorteResidual,
            int cantidadTotalCortes,
            BigDecimal factorSuelta,
            BigDecimal equivalencia
    ) {
        List<CalculoMedidasRespuestaDto.LineaComercialDto> resultado =
                new ArrayList<>();

        boolean piezaDescontada = false;

        Set<Integer> residuales =
                new HashSet<>(numerosCorteResidual);

        List<Integer> cortesFisicos =
                new ArrayList<>();

        for (int numero = 1;
             numero <= cantidadTotalCortes;
             numero++) {

            if (!residuales.contains(numero)) {
                cortesFisicos.add(numero);
            }
        }

        for (CalculoMedidasRespuestaDto.LineaComercialDto linea
                : lineasBase) {

            BigDecimal cantidad =
                    linea.cantidad();

            if (!piezaDescontada
                    && Objects.equals(
                    linea.idPrecioPresentacion(),
                    fisicaRetirada.presentacion
                            .getIdPrecioPresentacion()
            )) {
                cantidad =
                        cantidad.subtract(BigDecimal.ONE);

                piezaDescontada = true;
            }

            if (cantidad.signum() <= 0) {
                continue;
            }

            BigDecimal subtotal =
                    linea.precioUnitario()
                            .multiply(cantidad)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            resultado.add(
                    new CalculoMedidasRespuestaDto.LineaComercialDto(
                            linea.idPrecioPresentacion(),
                            linea.nombrePresentacion(),
                            linea.tipoCalculoVenta(),
                            cantidad,
                            linea.precioUnitario(),
                            subtotal,
                            linea.factorConversionStock(),
                            false,
                            List.copyOf(cortesFisicos)
                    )
            );
        }

        if (!piezaDescontada) {
            return List.of();
        }

        BigDecimal subtotalSuelta =
                suelta.getPrecioVenta()
                        .multiply(cantidadSuelta)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        resultado.add(
                new CalculoMedidasRespuestaDto.LineaComercialDto(
                        suelta.getIdPrecioPresentacion(),
                        suelta.getNombrePresentacion(),
                        suelta.getTipoCalculoVenta(),
                        cantidadSuelta,
                        suelta.getPrecioVenta(),
                        subtotalSuelta,
                        factorSuelta,
                        false,
                        List.copyOf(numerosCorteResidual),
                        equivalencia
                )
        );

        /*
         * Solamente la primera línea será la principal.
         */
        List<CalculoMedidasRespuestaDto.LineaComercialDto> normalizadas =
                new ArrayList<>();

        for (int i = 0; i < resultado.size(); i++) {
            CalculoMedidasRespuestaDto.LineaComercialDto linea =
                    resultado.get(i);

            normalizadas.add(
                    new CalculoMedidasRespuestaDto.LineaComercialDto(
                            linea.idPrecioPresentacion(),
                            linea.nombrePresentacion(),
                            linea.tipoCalculoVenta(),
                            linea.cantidad(),
                            linea.precioUnitario(),
                            linea.subtotal(),
                            linea.factorConversionStock(),
                            i == 0,
                            linea.numerosCorte(),
                            linea.equivalenciaUnidadBase()
                    )
            );
        }

        return normalizadas;
    }

    private BigDecimal calcularConsumoStockLineas(
            List<CalculoMedidasRespuestaDto.LineaComercialDto> lineas
    ) {
        return lineas.stream()
                .map(
                        linea ->
                                linea.factorConversionStock()
                                        .multiply(linea.cantidad())
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private BigDecimal calcularStockFisicoEquivalente(
            InventarioService.DisponibilidadFisicaProducto disponibilidad,
            BigDecimal respaldo
    ) {
        if (disponibilidad == null
                || disponibilidad.partesDisponibles() == null
                || disponibilidad.partesDisponibles().isEmpty()) {
            return respaldo == null
                    ? BigDecimal.ZERO
                    : respaldo.max(BigDecimal.ZERO);
        }

        int medidaTotal = Math.max(
                0,
                disponibilidad.medidaVendibleTotal()
        );

        int medidaBaseCompleta =
                disponibilidad.partesDisponibles().stream()
                        .filter(Objects::nonNull)
                        .filter(p ->
                                "COMPLETA".equalsIgnoreCase(
                                        p.getTipoParte()
                                )
                        )
                        .map(p -> p.getMedidaPorParte())
                        .filter(v -> v != null && v > 0)
                        .findFirst()
                        .orElse(0);

        if (medidaBaseCompleta <= 0) {
            return respaldo == null
                    ? BigDecimal.ZERO
                    : respaldo.max(BigDecimal.ZERO);
        }

        return BigDecimal.valueOf(medidaTotal)
                .divide(
                        BigDecimal.valueOf(medidaBaseCompleta),
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private List<Fisica> construirPiezasFisicasDisponibles(
            List<Fisica> fisicas,
            InventarioService.DisponibilidadFisicaProducto disponibilidad
    ) {
        if (fisicas == null || fisicas.isEmpty() || disponibilidad == null
                || disponibilidad.partesDisponibles() == null) {
            return List.of();
        }

        Fisica completa = fisicas.stream()
                .filter(f -> esPresentacionFisicaCompleta(f.presentacion()))
                .findFirst()
                .orElse(null);
        Fisica media = fisicas.stream()
                .filter(f -> esMediaPlancha(f.presentacion()))
                .findFirst()
                .orElse(null);

        List<Fisica> disponibles = new ArrayList<>();

        for (InventarioParte parte : disponibilidad.partesDisponibles()) {
            if (parte == null || parte.getIdParte() == null || parte.getIdParte() <= 0) {
                continue;
            }

            String estadoParte = normalizar(parte.getEstado());
            if (!"DISPONIBLE".equals(estadoParte)
                    && !"PARCIAL".equals(estadoParte)) {
                continue;
            }

            int cantidad = parte.getCantidadPartes() == null
                    ? 1 : Math.max(1, parte.getCantidadPartes());
            String tipoParte = normalizar(parte.getTipoParte());

            if ("RETAZO".equals(tipoParte) || "CORTE".equals(tipoParte)) {
                if (!positivo(parte.getLargoDisponibleMm())
                        || !positivo(parte.getAnchoDisponibleMm())) {
                    continue;
                }
                BigDecimal largo = parte.getLargoDisponibleMm();
                BigDecimal ancho = parte.getAnchoDisponibleMm();
                BigDecimal capacidad = largo.multiply(ancho)
                        .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);
                for (int i = 0; i < cantidad; i++) {
                    disponibles.add(new Fisica(
                            null, largo, ancho, null, capacidad,
                            parte.getEquivalenteUnidadFisica(),
                            parte.getIdParte(), tipoParte,
                            tipoParte + " #" + parte.getIdParte()
                    ));
                }
                continue;
            }

            Fisica base = "MEDIA".equals(tipoParte) ? media
                    : "COMPLETA".equals(tipoParte) ? completa : null;
            if (base == null) {
                continue;
            }
            for (int i = 0; i < cantidad; i++) {
                disponibles.add(base.conParteInventario(
                        parte.getIdParte(), tipoParte,
                        base.nombrePresentacion()
                ));
            }
        }

        disponibles.sort(Comparator
                .comparing((Fisica f) -> f.esRetazo() ? 0 : 1)
                .thenComparing(Fisica::capacidadCobro)
                .thenComparing(f -> f.idParteInventario() == null ? Integer.MAX_VALUE : f.idParteInventario()));

        return List.copyOf(disponibles);
    }

    private List<Bin> construirBinsFisicosDisponibles(
            List<Fisica> fisicas,
            InventarioService.DisponibilidadFisicaProducto disponibilidad
    ) {
        return construirPiezasFisicasDisponibles(
                fisicas,
                disponibilidad
        ).stream()
                .map(f ->
                        new Bin(
                                f.largo(),
                                f.ancho(),
                                f.longitud()
                        )
                )
                .toList();
    }

    private long contarRetazosConGeometria(InventarioService.DisponibilidadFisicaProducto disponibilidad) {
        if (disponibilidad == null || disponibilidad.partesDisponibles() == null) return 0;
        return disponibilidad.partesDisponibles().stream()
                .filter(Objects::nonNull)
                .filter(p -> "RETAZO".equalsIgnoreCase(p.getTipoParte()) || "CORTE".equalsIgnoreCase(p.getTipoParte()))
                .filter(p -> positivo(p.getLargoDisponibleMm()) && positivo(p.getAnchoDisponibleMm()))
                .count();
    }

    private long contarRetazosSinGeometria(InventarioService.DisponibilidadFisicaProducto disponibilidad) {
        if (disponibilidad == null || disponibilidad.partesDisponibles() == null) return 0;
        return disponibilidad.partesDisponibles().stream()
                .filter(Objects::nonNull)
                .filter(p -> "RETAZO".equalsIgnoreCase(p.getTipoParte()) || "CORTE".equalsIgnoreCase(p.getTipoParte()))
                .filter(p -> !positivo(p.getLargoDisponibleMm()) || !positivo(p.getAnchoDisponibleMm()))
                .count();
    }

    private void validarCortesContraInventarioFisico(
            List<Fisica> fisicas,
            List<Corte> cortes,
            String tipo,
            InventarioService.DisponibilidadFisicaProducto disponibilidad
    ) {
        if (disponibilidad == null || !disponibilidad.tienePartes()) {
            return;
        }

        List<Bin> binsDisponibles =
                construirBinsFisicosDisponibles(
                        fisicas,
                        disponibilidad
                );

        if (binsDisponibles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No existen planchas completas ni medias planchas "
                            + "con dimensiones conocidas para realizar los cortes."
            );
        }

        if (!caben(cortes, binsDisponibles, tipo)) {
            int completas = Math.max(
                    0,
                    disponibilidad.completasDisponibles()
            );

            int medias = Math.max(
                    0,
                    disponibilidad.mediasDisponibles()
            );

            throw new IllegalArgumentException(
                    "Los cortes no pueden distribuirse físicamente en el material disponible. "
                            + "Planchas completas: " + completas
                            + ", medias planchas directas: " + medias
                            + ", retazos con dimensiones: " + contarRetazosConGeometria(disponibilidad)
                            + ", retazos sin dimensiones: " + contarRetazosSinGeometria(disponibilidad) + "."
            );
        }
    }

    private List<Fisica> reducirPiezasFisicasNecesarias(
            List<Fisica> disponibles,
            List<Corte> cortes,
            String tipo
    ) {
        List<Fisica> seleccionadas = new ArrayList<>(disponibles);

        seleccionadas.sort(
                Comparator.comparing(Fisica::capacidadCobro)
        );

        boolean seRetiro;

        do {
            seRetiro = false;

            for (int i = seleccionadas.size() - 1; i >= 0; i--) {
                List<Fisica> prueba = new ArrayList<>(seleccionadas);
                prueba.remove(i);

                if (prueba.isEmpty()) {
                    continue;
                }

                List<Bin> bins = prueba.stream()
                        .map(f -> new Bin(
                                f.largo(),
                                f.ancho(),
                                f.longitud()
                        ))
                        .toList();

                if (caben(cortes, bins, tipo)) {
                    seleccionadas = prueba;
                    seRetiro = true;
                    break;
                }
            }

        } while (seRetiro);

        return List.copyOf(seleccionadas);
    }

    private int maximoDisponibleParaPresentacion(
            Fisica fisica,
            InventarioService.DisponibilidadFisicaProducto disponibilidad,
            BigDecimal stockDisponible
    ) {
        if (fisica == null) {
            return 0;
        }

        if (disponibilidad != null && disponibilidad.tienePartes()) {
            if (esPresentacionFisicaCompleta(fisica.presentacion())) {
                return Math.max(
                        0,
                        disponibilidad.completasDisponibles()
                );
            }

            if (esMediaPlancha(fisica.presentacion())) {
                int mediasDirectas = Math.max(
                        0,
                        disponibilidad.mediasDisponibles()
                );

                int completasDisponibles = Math.max(
                        0,
                        disponibilidad.completasDisponibles()
                );

                return mediasDirectas + (completasDisponibles * 2);
            }
        }

        if (fisica.factorStock() == null
                || fisica.factorStock().signum() <= 0
                || stockDisponible == null
                || stockDisponible.signum() <= 0) {
            return 0;
        }

        return stockDisponible
                .divide(
                        fisica.factorStock(),
                        0,
                        RoundingMode.FLOOR
                )
                .intValue();
    }

    private boolean hayDisponibilidadFisicaParaCombinacion(
            int[] cantidades,
            List<Fisica> fisicas,
            InventarioService.DisponibilidadFisicaProducto disponibilidad
    ) {
        if (cantidades == null
                || fisicas == null
                || cantidades.length != fisicas.size()) {
            return false;
        }

        if (disponibilidad == null || !disponibilidad.tienePartes()) {
            return true;
        }

        int completasSolicitadas = 0;
        int mediasSolicitadas = 0;

        for (int i = 0; i < cantidades.length; i++) {
            int cantidad = cantidades[i];

            if (cantidad <= 0) {
                continue;
            }

            ProductoPrecioPresentacion presentacion =
                    fisicas.get(i).presentacion();

            if (esPresentacionFisicaCompleta(presentacion)) {
                completasSolicitadas += cantidad;
            } else if (esMediaPlancha(presentacion)) {
                mediasSolicitadas += cantidad;
            }
        }

        int completasDisponibles = Math.max(
                0,
                disponibilidad.completasDisponibles()
        );

        int mediasDisponibles = Math.max(
                0,
                disponibilidad.mediasDisponibles()
        );

        /*
         * Primero se reservan las planchas que se venderán completas.
         */
        if (completasSolicitadas > completasDisponibles) {
            return false;
        }

        int completasRestantes =
                completasDisponibles - completasSolicitadas;

        /*
         * Se usan primero las medias que ya existen.
         */
        int mediasFaltantes =
                Math.max(0, mediasSolicitadas - mediasDisponibles);

        /*
         * Una completa puede producir como máximo dos medias.
         */
        int completasNecesariasParaMedias =
                (mediasFaltantes + 1) / 2;

        return completasNecesariasParaMedias
                <= completasRestantes;
    }

    private void enumerar(
            int idx,
            int[] counts,
            int[] max,
            List<Fisica> fisicas,
            List<Corte> cortes,
            String tipo,
            BigDecimal stockDisponible,
            InventarioService.DisponibilidadFisicaProducto disponibilidadFisica,
            List<CalculoMedidasRespuestaDto.CombinacionDto> out,
            int[] visitadas
    ) {
        if (visitadas[0] >= MAX_CANDIDATAS) return;
        if (idx == counts.length) {
            visitadas[0]++;
            int total = Arrays.stream(counts).sum();
            if (total == 0) return;
            BigDecimal consumoStock = BigDecimal.ZERO;
            List<Bin> bins = new ArrayList<>();
            List<CalculoMedidasRespuestaDto.LineaComercialDto> lineas = new ArrayList<>();
            BigDecimal precio = BigDecimal.ZERO;
            BigDecimal capacidad = BigDecimal.ZERO;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                Fisica f = fisicas.get(i);
                BigDecimal cantidad = BigDecimal.valueOf(counts[i]);
                consumoStock = consumoStock.add(f.factorStock.multiply(cantidad));
                if (consumoStock.compareTo(stockDisponible) > 0) return;
                for (int k = 0; k < counts[i]; k++) bins.add(new Bin(f.largo, f.ancho, f.longitud));
                BigDecimal subtotal = f.presentacion.getPrecioVenta().multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
                precio = precio.add(subtotal);
                capacidad = capacidad.add(f.capacidadCobro.multiply(cantidad));
                lineas.add(new CalculoMedidasRespuestaDto.LineaComercialDto(
                        f.presentacion.getIdPrecioPresentacion(), f.presentacion.getNombrePresentacion(),
                        f.presentacion.getTipoCalculoVenta(), cantidad, f.presentacion.getPrecioVenta(), subtotal,
                        f.factorStock, lineas.isEmpty()));
            }

            if (!hayDisponibilidadFisicaParaCombinacion(
                    counts,
                    fisicas,
                    disponibilidadFisica
            )) {
                return;
            }
            if (!caben(cortes, bins, tipo)) {
                return;
            }

            /*
             * No se acepta una combinación si contiene alguna presentación física
             * innecesaria. Si al retirar una pieza todos los cortes siguen cabiendo,
             * esa combinación está sobredimensionada y generaría un plano vacío.
             */
            /*
            if (!esCombinacionFisicaMinima(cortes, bins, tipo)) {
                return;
            }
            */

            out.add(new CalculoMedidasRespuestaDto.CombinacionDto(
                    codigo(counts, fisicas),
                    "Combinación físicamente válida, disponible en sede y sin presentaciones innecesarias.",
                    precio.setScale(2, RoundingMode.HALF_UP),
                    capacidad.setScale(10, RoundingMode.HALF_UP),
                    total,
                    lineas
            ));
            return;
        }
        for (int c = 0; c <= max[idx]; c++) {
            counts[idx] = c;
            enumerar(
                    idx + 1,
                    counts,
                    max,
                    fisicas,
                    cortes,
                    tipo,
                    stockDisponible,
                    disponibilidadFisica,
                    out,
                    visitadas
            );
            if (visitadas[0] >= MAX_CANDIDATAS) break;
        }
        counts[idx] = 0;
    }

    private boolean esCombinacionFisicaMinima(
            List<Corte> cortes,
            List<Bin> bins,
            String tipo
    ) {
        if (bins == null || bins.isEmpty()) {
            return false;
        }

        /*
         * Si existe más de una pieza física, probamos retirar cada una.
         * Si los cortes todavía caben, esa pieza era innecesaria.
         */
        for (int i = 0; i < bins.size(); i++) {
            List<Bin> reducidos = new ArrayList<>(bins);
            reducidos.remove(i);

            if (!reducidos.isEmpty() && caben(cortes, reducidos, tipo)) {
                return false;
            }
        }

        return true;
    }

    private boolean caben(List<Corte> cortes, List<Bin> bins, String tipo) {
        if (tipo.equals("LONGITUD")) {
            List<BigDecimal> restantes = bins.stream().map(b -> b.longitud).sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());
            List<Corte> orden = cortes.stream().sorted(Comparator.comparing(Corte::longitud).reversed()).toList();
            for (Corte c : orden) {
                int mejor = -1;
                BigDecimal menorResto = null;
                for (int i = 0; i < restantes.size(); i++) {
                    if (restantes.get(i).compareTo(c.longitud) >= 0) {
                        BigDecimal resto = restantes.get(i).subtract(c.longitud);
                        if (menorResto == null || resto.compareTo(menorResto) < 0) { mejor = i; menorResto = resto; }
                    }
                }
                if (mejor < 0) return false;
                restantes.set(mejor, menorResto);
            }
            return true;
        }
        for (List<Corte> orden : ordenesArea(cortes)) {
            List<RectBin> rectBins = bins.stream()
                    .map(b -> new RectBin(b.largo, b.ancho))
                    .collect(java.util.stream.Collectors.toList());
            boolean completo = true;
            for (Corte corte : orden) {
                boolean puesto = false;
                for (RectBin bin : rectBins) {
                    if (bin.colocar(corte)) {
                        puesto = true;
                        break;
                    }
                }
                if (!puesto) {
                    completo = false;
                    break;
                }
            }
            if (completo) return true;
        }
        return false;
    }

    private List<List<Corte>> ordenesArea(List<Corte> cortes) {
        List<Comparator<Corte>> comparadores = List.of(
                Comparator.comparing((Corte c) -> c.ancho.multiply(c.alto)).reversed()
                        .thenComparing(c -> c.ancho.max(c.alto), Comparator.reverseOrder())
                        .thenComparing(c -> c.ancho.min(c.alto), Comparator.reverseOrder()),
                Comparator.comparing((Corte c) -> c.ancho.max(c.alto)).reversed()
                        .thenComparing(c -> c.ancho.multiply(c.alto), Comparator.reverseOrder()),
                Comparator.comparing((Corte c) -> c.ancho).reversed()
                        .thenComparing(c -> c.alto, Comparator.reverseOrder()),
                Comparator.comparing((Corte c) -> c.alto).reversed()
                        .thenComparing(c -> c.ancho, Comparator.reverseOrder())
        );
        List<List<Corte>> ordenes = new ArrayList<>();
        for (Comparator<Corte> comparador : comparadores) {
            List<Corte> orden = cortes.stream().sorted(comparador).toList();
            if (!ordenes.contains(orden)) ordenes.add(orden);
        }
        List<Corte> original = List.copyOf(cortes);
        if (!ordenes.contains(original)) ordenes.add(original);
        return ordenes;
    }

    private List<List<CorteNumerado>> ordenesAreaNumerados(List<CorteNumerado> cortes) {
        List<Comparator<CorteNumerado>> comparadores = List.of(
                Comparator.comparing((CorteNumerado c) -> c.corte.ancho.multiply(c.corte.alto)).reversed()
                        .thenComparing(c -> c.corte.ancho.max(c.corte.alto), Comparator.reverseOrder())
                        .thenComparingInt(CorteNumerado::numero),
                Comparator.comparing((CorteNumerado c) -> c.corte.ancho.max(c.corte.alto)).reversed()
                        .thenComparing(c -> c.corte.ancho.multiply(c.corte.alto), Comparator.reverseOrder())
                        .thenComparingInt(CorteNumerado::numero),
                Comparator.comparing((CorteNumerado c) -> c.corte.ancho).reversed()
                        .thenComparing(c -> c.corte.alto, Comparator.reverseOrder())
                        .thenComparingInt(CorteNumerado::numero),
                Comparator.comparing((CorteNumerado c) -> c.corte.alto).reversed()
                        .thenComparing(c -> c.corte.ancho, Comparator.reverseOrder())
                        .thenComparingInt(CorteNumerado::numero)
        );
        List<List<CorteNumerado>> ordenes = new ArrayList<>();
        for (Comparator<CorteNumerado> comparador : comparadores) {
            List<CorteNumerado> orden = cortes.stream().sorted(comparador).toList();
            if (!ordenes.contains(orden)) ordenes.add(orden);
        }
        List<CorteNumerado> original = List.copyOf(cortes);
        if (!ordenes.contains(original)) ordenes.add(original);
        return ordenes;
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoCorte(
            CalculoMedidasRespuestaDto.CombinacionDto recomendada,
            List<CalculoMedidasRespuestaDto.CombinacionDto> candidatas,
            List<Fisica> fisicas,
            List<Corte> cortes,
            String tipo,
            String unidad,
            boolean entregarSobrante,
            InventarioService.DisponibilidadFisicaProducto disponibilidadFisica
    ) {
        boolean soloMedida = recomendada.lineas() != null
                && !recomendada.lineas().isEmpty()
                && recomendada.lineas().stream().allMatch(linea -> esSueltaTipo(linea.tipoCalculoVenta()));
        if (soloMedida) {
            CalculoMedidasRespuestaDto.LineaComercialDto lineaComercial =
                    recomendada.lineas().stream()
                            .filter(linea ->
                                    esSueltaTipo(
                                            linea.tipoCalculoVenta()
                                    )
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "No se encontró la presentación comercial "
                                                    + "cobrada por medida."
                                    )
                            );

            if (disponibilidadFisica != null && disponibilidadFisica.tienePartes()) {
                List<Fisica> piezasDisponibles =
                        construirPiezasFisicasDisponibles(
                                fisicas,
                                disponibilidadFisica
                        );

                if (piezasDisponibles.isEmpty()) {
                    throw new IllegalArgumentException(
                            "No existen piezas físicas conocidas para realizar "
                                    + "los cortes cobrados por medida."
                    );
                }

                List<Bin> binsDisponibles = piezasDisponibles.stream()
                        .map(f -> new Bin(
                                f.largo(),
                                f.ancho(),
                                f.longitud()
                        ))
                        .toList();

                if (!caben(cortes, binsDisponibles, tipo)) {
                    throw new IllegalArgumentException(
                            "Los cortes no caben físicamente en las piezas "
                                    + "disponibles del inventario."
                    );
                }

                List<Fisica> piezasSeleccionadas =
                        seleccionarPiezasFisicasParaCorteExacto(
                                piezasDisponibles,
                                cortes,
                                tipo
                        );

                CalculoMedidasRespuestaDto.PlanoCorteDto planoExacto =
                        tipo.equals("AREA")
                                ? construirPlanoArea(
                                piezasSeleccionadas,
                                cortes,
                                unidad,
                                false
                        )
                                : construirPlanoLongitud(
                                piezasSeleccionadas,
                                cortes,
                                unidad,
                                false
                        );

                return convertirPlanoACompraPorMedidaExacta(
                        planoExacto,
                        lineaComercial
                );
            }

            if (tipo.equals("AREA")) {
                List<Integer> numeros = java.util.stream.IntStream.rangeClosed(1, cortes.size()).boxed().toList();
                return construirPlanoComercialArea(lineaComercial, numeros, cortes, unidad, fisicas);
            } else {
                List<Integer> numeros = java.util.stream.IntStream.rangeClosed(1, cortes.size()).boxed().toList();
                return construirPlanoComercialLongitud(lineaComercial, numeros, cortes, unidad);
            }
        }

        /*
         * Las combinaciones residuales tienen planos separados:
         * - planos físicos para plancha completa/media plancha;
         * - un plano comercial para los cortes cobrados por pie² o m².
         */
        if (recomendada.codigo() != null
                && recomendada.codigo().startsWith("RESIDUAL|")) {

            if (!tipo.equals("AREA")) {
                throw new IllegalArgumentException(
                        "Las combinaciones residuales solo están disponibles para productos de área."
                );
            }

            return construirPlanoResidualArea(
                    recomendada,
                    candidatas,
                    fisicas,
                    cortes,
                    unidad,
                    entregarSobrante,
                    disponibilidadFisica
            );
        }

        CalculoMedidasRespuestaDto.CombinacionDto fuenteFisica =
                recomendada;

        if (recomendada.codigo() != null
                && recomendada.codigo().startsWith("SUELTA-")) {
            fuenteFisica = candidatas.stream()
                    .filter(c ->
                            c.codigo() != null
                                    && c.codigo().startsWith("MIX")
                    )
                    .min(
                            Comparator
                                    .comparing(
                                            CalculoMedidasRespuestaDto
                                                    .CombinacionDto::materialAdquirido
                                    )
                                    .thenComparingInt(
                                            CalculoMedidasRespuestaDto
                                                    .CombinacionDto::piezasFisicas
                                    )
                                    .thenComparing(
                                            CalculoMedidasRespuestaDto
                                                    .CombinacionDto::precioTotal
                                    )
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "No se encontró una fuente física para dibujar los cortes."
                            )
                    );
        }

        List<Fisica> piezasComerciales =
                expandirPiezasFisicas(
                        fuenteFisica,
                        fisicas
                );

        if (piezasComerciales.isEmpty()) {
            throw new IllegalArgumentException(
                    "La combinación recomendada no contiene piezas físicas para dibujar."
            );
        }

        List<Fisica> piezasInventario = construirPiezasFisicasDisponibles(
                fisicas,
                disponibilidadFisica
        );

        CalculoMedidasRespuestaDto.PlanoCorteDto planoMaterializado;

        if (piezasInventario == null || piezasInventario.isEmpty()) {
            CalculoMedidasRespuestaDto.PlanoCorteDto planoComercial =
                    tipo.equals("AREA")
                            ? construirPlanoArea(
                            piezasComerciales,
                            cortes,
                            unidad,
                            entregarSobrante
                    )
                            : construirPlanoLongitud(
                            piezasComerciales,
                            cortes,
                            unidad,
                            entregarSobrante
                    );
            planoMaterializado = planoComercial;
        } else {
            List<AsignacionFisica> asignaciones =
                    asignarPartesInventarioACombinacion(
                            piezasComerciales,
                            piezasInventario,
                            tipo
                    );

            List<Fisica> geometriaComercial = asignaciones.stream()
                    .map(AsignacionFisica::piezaComercial)
                    .toList();

            CalculoMedidasRespuestaDto.PlanoCorteDto planoComercial =
                    tipo.equals("AREA")
                            ? construirPlanoArea(
                            geometriaComercial,
                            cortes,
                            unidad,
                            entregarSobrante
                    )
                            : construirPlanoLongitud(
                            geometriaComercial,
                            cortes,
                            unidad,
                            entregarSobrante
                    );

            planoMaterializado =
                    materializarPlanoConPartesInventario(
                            planoComercial,
                            asignaciones,
                            tipo
                    );
        }

        /*
         * La política se aplica por pieza y por tipo comercial, no por la
         * modalidad original. Esto permite combinaciones como MEDIA + PIE2
         * o COMPLETA + PIE2 sin mezclar la propiedad de los sobrantes.
         */
        return aplicarPoliticaPresentacionComprada(
                planoMaterializado,
                entregarSobrante,
                tipo
        );
    }



    private CalculoMedidasRespuestaDto.PlanoCorteDto aplicarPoliticaPresentacionComprada(
            CalculoMedidasRespuestaDto.PlanoCorteDto plano,
            boolean entregarSobrante,
            String tipo
    ) {
        if (plano == null || plano.piezas() == null) {
            return plano;
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezas = new ArrayList<>();

        for (CalculoMedidasRespuestaDto.PiezaFisicaDto pieza : plano.piezas()) {
            CalculoMedidasRespuestaDto.BloqueComercialDto bloque = pieza.bloqueComercial();
            if (bloque == null) {
                piezas.add(pieza);
                continue;
            }

            String tipoBloque = normalizar(bloque.tipoBloque());
            String unidadComercial = normalizar(
                    bloque.unidadComercial() != null
                            ? bloque.unidadComercial()
                            : pieza.unidadComercial()
            );
            String nombre = normalizar(pieza.nombrePresentacion());

            boolean esMedia = tipoBloque.equals("MEDIA")
                    || unidadComercial.equals("MEDIA")
                    || nombre.contains("MEDIA PLANCHA")
                    || nombre.contains("1/2 PLANCHA");

            boolean esCompleta = tipoBloque.equals("COMPLETA")
                    || unidadComercial.equals("COMPLETA")
                    || nombre.contains("PLANCHA COMPLETA")
                    || nombre.contains("VARILLA COMPLETA")
                    || nombre.contains("ROLLO COMPLETO");

            /*
             * PIE2, M2 y METRO_LINEAL conservan su propia política:
             * sobrante útil para DentIA y merma según el mínimo.
             */
            if (!esMedia && !esCompleta) {
                piezas.add(pieza);
                continue;
            }

            List<CalculoMedidasRespuestaDto.SobranteDto> internos =
                    bloque.sobrantes() == null
                            ? List.of()
                            : bloque.sobrantes().stream()
                            .map(s -> new CalculoMedidasRespuestaDto.SobranteDto(
                                    s.numero(), s.x(), s.y(), s.ancho(), s.alto(), s.largo(), s.medida(),
                                    entregarSobrante,
                                    entregarSobrante ? "CLIENTE_ENTREGAR" : "MERMA_DESECHAR"
                            ))
                            .toList();

            List<CalculoMedidasRespuestaDto.SobranteDto> externos =
                    pieza.sobrantes() == null
                            ? List.of()
                            : pieza.sobrantes().stream()
                            .map(s -> new CalculoMedidasRespuestaDto.SobranteDto(
                                    s.numero(), s.x(), s.y(), s.ancho(), s.alto(), s.largo(), s.medida(),
                                    false,
                                    esMedia ? "DentIA_NO_VENDIDO" : s.clasificacion()
                            ))
                            .toList();

            List<CalculoMedidasRespuestaDto.SobranteDto> todos = new ArrayList<>();
            todos.addAll(internos);
            todos.addAll(externos);

            CalculoMedidasRespuestaDto.BloqueComercialDto bloqueActualizado =
                    new CalculoMedidasRespuestaDto.BloqueComercialDto(
                            bloque.tipoBloque(), bloque.x(), bloque.y(), bloque.ancho(), bloque.alto(), bloque.largo(),
                            bloque.cantidadComercial(), bloque.unidadComercial(), bloque.cortes(), internos,
                            bloque.medidaUtilizada(), bloque.medidaSobrante(), bloque.aprovechamientoPorcentaje()
                    );

            piezas.add(new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                    pieza.numero(), pieza.codigo(), pieza.idPrecioPresentacion(), pieza.nombrePresentacion(),
                    pieza.idParteInventario(), pieza.tipoParteInventario(), "FISICO",
                    pieza.ancho(), pieza.alto(), pieza.largo(), pieza.cantidadComercial(), pieza.unidadComercial(),
                    bloqueActualizado, bloque.cortes(), List.copyOf(todos),
                    bloque.medidaUtilizada(), bloque.medidaSobrante(), bloque.aprovechamientoPorcentaje()
            ));
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                plano.tipoMedida(), plano.unidad(), List.copyOf(piezas)
        );
    }

    private List<Fisica> seleccionarPiezasFisicasParaCorteExacto(
            List<Fisica> disponibles,
            List<Corte> cortes,
            String tipo
    ) {
        if (disponibles == null || disponibles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No existen piezas físicas disponibles para realizar el corte."
            );
        }

        Comparator<Fisica> comparador = tipo.equals("AREA")
                ? Comparator.comparing(f -> f.largo().multiply(f.ancho()))
                : Comparator.comparing(Fisica::longitud);

        List<Fisica> ordenadas = disponibles.stream()
                .filter(Objects::nonNull)
                .sorted(comparador)
                .toList();

        // Primero intenta resolver todo usando una sola pieza: reduce cortes y retazos.
        for (Fisica pieza : ordenadas) {
            List<Bin> bins = List.of(new Bin(
                    pieza.largo(),
                    pieza.ancho(),
                    pieza.longitud()
            ));
            if (caben(cortes, bins, tipo)) {
                return List.of(pieza);
            }
        }

        // Cuando una sola pieza no alcanza, agrega las más pequeñas hasta completar.
        List<Fisica> seleccionadas = new ArrayList<>();
        for (Fisica pieza : ordenadas) {
            seleccionadas.add(pieza);
            List<Bin> bins = seleccionadas.stream()
                    .map(f -> new Bin(f.largo(), f.ancho(), f.longitud()))
                    .toList();
            if (caben(cortes, bins, tipo)) {
                return List.copyOf(seleccionadas);
            }
        }

        throw new IllegalArgumentException(
                "Los cortes solicitados no caben en las piezas físicas disponibles."
        );
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto convertirPlanoACompraPorMedidaExacta(
            CalculoMedidasRespuestaDto.PlanoCorteDto plano,
            CalculoMedidasRespuestaDto.LineaComercialDto lineaComercial
    ) {
        if (plano == null || plano.piezas() == null) {
            throw new IllegalArgumentException("No se generó el plano físico exacto.");
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezas =
                plano.piezas().stream()
                        .map(pieza -> {
                            CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                                    pieza.bloqueComercial();

                            List<CalculoMedidasRespuestaDto.CorteUbicadoDto> cortes =
                                    bloque != null && bloque.cortes() != null
                                            ? bloque.cortes()
                                            : pieza.cortes();

                            // No consumir piezas seleccionadas que finalmente no recibieron cortes.
                            if (cortes == null || cortes.isEmpty()) {
                                return null;
                            }

                            List<CalculoMedidasRespuestaDto.SobranteDto> sobrantes =
                                    bloque != null && bloque.sobrantes() != null
                                            ? bloque.sobrantes()
                                            : pieza.sobrantes();

                            BigDecimal utilizada = bloque != null
                                    ? bloque.medidaUtilizada()
                                    : pieza.medidaUtilizada();
                            BigDecimal sobrante = bloque != null
                                    ? bloque.medidaSobrante()
                                    : pieza.medidaSobrante();
                            BigDecimal aprovechamiento = bloque != null
                                    ? bloque.aprovechamientoPorcentaje()
                                    : pieza.aprovechamientoPorcentaje();

                            return new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                                    pieza.numero(),
                                    pieza.codigo(),
                                    lineaComercial.idPrecioPresentacion(),
                                    lineaComercial.nombrePresentacion(),
                                    pieza.idParteInventario(),
                                    pieza.tipoParteInventario(),
                                    "COMERCIAL_MEDIDA",
                                    pieza.ancho(),
                                    pieza.alto(),
                                    pieza.largo(),
                                    lineaComercial.cantidad(),
                                    lineaComercial.tipoCalculoVenta(),
                                    null,
                                    List.copyOf(cortes),
                                    sobrantes == null ? List.of() : List.copyOf(sobrantes),
                                    utilizada == null ? BigDecimal.ZERO : utilizada,
                                    sobrante == null ? BigDecimal.ZERO : sobrante,
                                    aprovechamiento == null ? BigDecimal.ZERO : aprovechamiento
                            );
                        })
                        .filter(Objects::nonNull)
                        .toList();

        if (piezas.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se asignó ningún corte a una pieza física del inventario."
            );
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                plano.tipoMedida(),
                plano.unidad(),
                piezas
        );
    }

    private List<AsignacionFisica> asignarPartesInventarioACombinacion(
            List<Fisica> piezasComerciales,
            List<Fisica> piezasInventario,
            String tipo
    ) {
        if (piezasInventario == null || piezasInventario.isEmpty()) {
            throw new IllegalArgumentException(
                    "No existen partes físicas de inventario disponibles para la combinación seleccionada."
            );
        }

        List<Fisica> disponibles = new ArrayList<>(piezasInventario);
        List<AsignacionFisica> resultado = new ArrayList<>();

        for (Fisica comercial : piezasComerciales) {
            int indiceExacto = buscarParteInventarioExacta(
                    comercial,
                    disponibles,
                    tipo
            );

            if (indiceExacto >= 0) {
                Fisica origen = disponibles.remove(indiceExacto);
                resultado.add(new AsignacionFisica(
                        comercial,
                        origen,
                        false
                ));
                continue;
            }

            /*
             * Una media plancha puede obtenerse de una plancha completa.
             * En ese caso se consume una unidad completa del lote y la otra
             * mitad queda registrada como material exterior de DentIA.
             */
            if (esMediaPlancha(comercial.presentacion())) {
                int indiceCompleta = buscarParteCompletaContenedora(
                        comercial,
                        disponibles,
                        tipo
                );

                if (indiceCompleta >= 0) {
                    Fisica origenCompleto = disponibles.remove(indiceCompleta);
                    resultado.add(new AsignacionFisica(
                            comercial,
                            origenCompleto,
                            true
                    ));
                    continue;
                }
            }

            throw new IllegalArgumentException(
                    "No existe una parte física real de inventario para "
                            + comercial.nombrePresentacion()
                            + "."
            );
        }

        return List.copyOf(resultado);
    }

    private int buscarParteInventarioExacta(
            Fisica comercial,
            List<Fisica> disponibles,
            String tipo
    ) {
        int mejorIndice = -1;
        BigDecimal mejorCapacidad = null;

        for (int i = 0; i < disponibles.size(); i++) {
            Fisica candidata = disponibles.get(i);

            if (candidata.idParteInventario() == null
                    || candidata.idParteInventario() <= 0) {
                continue;
            }

            boolean mismoTipo =
                    (esPresentacionFisicaCompleta(comercial.presentacion())
                            && "COMPLETA".equalsIgnoreCase(candidata.tipoParteInventario()))
                            || (esMediaPlancha(comercial.presentacion())
                            && "MEDIA".equalsIgnoreCase(candidata.tipoParteInventario()))
                            || comercial.esRetazo();

            if (!mismoTipo || !contieneFisicamente(candidata, comercial, tipo)) {
                continue;
            }

            if (mejorIndice < 0
                    || candidata.capacidadCobro().compareTo(mejorCapacidad) < 0) {
                mejorIndice = i;
                mejorCapacidad = candidata.capacidadCobro();
            }
        }

        return mejorIndice;
    }

    private int buscarParteCompletaContenedora(
            Fisica media,
            List<Fisica> disponibles,
            String tipo
    ) {
        for (int i = 0; i < disponibles.size(); i++) {
            Fisica candidata = disponibles.get(i);

            if (candidata.idParteInventario() == null
                    || candidata.idParteInventario() <= 0
                    || !"COMPLETA".equalsIgnoreCase(candidata.tipoParteInventario())) {
                continue;
            }

            if (contieneFisicamente(candidata, media, tipo)) {
                return i;
            }
        }

        return -1;
    }

    private boolean contieneFisicamente(
            Fisica contenedora,
            Fisica requerida,
            String tipo
    ) {
        if ("LONGITUD".equals(tipo)) {
            return positivo(contenedora.longitud())
                    && positivo(requerida.longitud())
                    && contenedora.longitud().compareTo(requerida.longitud()) >= 0;
        }

        if (!positivo(contenedora.largo())
                || !positivo(contenedora.ancho())
                || !positivo(requerida.largo())
                || !positivo(requerida.ancho())) {
            return false;
        }

        boolean normal =
                requerida.largo().compareTo(contenedora.largo()) <= 0
                        && requerida.ancho().compareTo(contenedora.ancho()) <= 0;

        boolean rotada =
                requerida.ancho().compareTo(contenedora.largo()) <= 0
                        && requerida.largo().compareTo(contenedora.ancho()) <= 0;

        return normal || rotada;
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto materializarPlanoConPartesInventario(
            CalculoMedidasRespuestaDto.PlanoCorteDto planoComercial,
            List<AsignacionFisica> asignaciones,
            String tipo
    ) {
        if (planoComercial == null
                || planoComercial.piezas() == null
                || planoComercial.piezas().size() != asignaciones.size()) {
            throw new IllegalArgumentException(
                    "No fue posible relacionar el plano comercial con las partes físicas del inventario."
            );
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezas = new ArrayList<>();

        for (int i = 0; i < planoComercial.piezas().size(); i++) {
            CalculoMedidasRespuestaDto.PiezaFisicaDto comercial =
                    planoComercial.piezas().get(i);
            AsignacionFisica asignacion = asignaciones.get(i);
            Fisica origen = asignacion.parteInventario();

            if (origen.idParteInventario() == null
                    || origen.idParteInventario() <= 0) {
                throw new IllegalArgumentException(
                        "El plano " + comercial.codigo()
                                + " no tiene una parte física real asignada."
                );
            }

            if ("AREA".equals(tipo)) {
                piezas.add(materializarPiezaArea(
                        comercial,
                        asignacion
                ));
            } else {
                piezas.add(materializarPiezaLongitud(
                        comercial,
                        asignacion
                ));
            }
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                planoComercial.tipoMedida(),
                planoComercial.unidad(),
                List.copyOf(piezas)
        );
    }

    private CalculoMedidasRespuestaDto.PiezaFisicaDto materializarPiezaArea(
            CalculoMedidasRespuestaDto.PiezaFisicaDto comercial,
            AsignacionFisica asignacion
    ) {
        Fisica origen = asignacion.parteInventario();
        CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                comercial.bloqueComercial();

        if (bloque == null) {
            bloque = new CalculoMedidasRespuestaDto.BloqueComercialDto(
                    asignacion.derivadaDeCompleta() ? "MEDIA" : "FISICO",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    comercial.ancho(),
                    comercial.alto(),
                    null,
                    comercial.cantidadComercial(),
                    comercial.unidadComercial(),
                    comercial.cortes(),
                    comercial.sobrantes(),
                    comercial.medidaUtilizada(),
                    comercial.medidaSobrante(),
                    comercial.aprovechamientoPorcentaje()
            );
        }

        List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesDentIA =
                asignacion.derivadaDeCompleta()
                        ? construirSobrantesDentIAFueraBloqueArea(origen, bloque)
                        : List.of();

        BigDecimal areaOrigenM2 = origen.largo()
                .multiply(origen.ancho())
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);

        BigDecimal areaBloqueM2 = bloque.ancho()
                .multiply(bloque.alto())
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);

        BigDecimal restanteM2 = areaOrigenM2.subtract(areaBloqueM2)
                .max(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal aprovechamiento = areaOrigenM2.signum() <= 0
                ? BigDecimal.ZERO
                : areaBloqueM2.multiply(BigDecimal.valueOf(100))
                .divide(areaOrigenM2, 2, RoundingMode.HALF_UP);

        return new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                comercial.numero(),
                comercial.codigo(),
                comercial.idPrecioPresentacion(),
                comercial.nombrePresentacion(),
                origen.idParteInventario(),
                origen.tipoParteInventario(),
                "FISICO_CON_BLOQUE_COMERCIAL",
                origen.largo(),
                origen.ancho(),
                null,
                comercial.cantidadComercial(),
                comercial.unidadComercial(),
                bloque,
                bloque.cortes(),
                List.copyOf(sobrantesDentIA),
                areaBloqueM2,
                restanteM2,
                aprovechamiento
        );
    }

    private CalculoMedidasRespuestaDto.PiezaFisicaDto materializarPiezaLongitud(
            CalculoMedidasRespuestaDto.PiezaFisicaDto comercial,
            AsignacionFisica asignacion
    ) {
        Fisica origen = asignacion.parteInventario();
        CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                comercial.bloqueComercial();

        if (bloque == null) {
            bloque = new CalculoMedidasRespuestaDto.BloqueComercialDto(
                    asignacion.derivadaDeCompleta() ? "MEDIA" : "FISICO",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    null,
                    comercial.largo(),
                    comercial.cantidadComercial(),
                    comercial.unidadComercial(),
                    comercial.cortes(),
                    comercial.sobrantes(),
                    comercial.medidaUtilizada(),
                    comercial.medidaSobrante(),
                    comercial.aprovechamientoPorcentaje()
            );
        }

        BigDecimal restanteCm = origen.longitud()
                .subtract(bloque.largo())
                .max(BigDecimal.ZERO);

        List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesDentIA =
                new ArrayList<>();

        if (asignacion.derivadaDeCompleta() && restanteCm.signum() > 0) {
            sobrantesDentIA.add(new CalculoMedidasRespuestaDto.SobranteDto(
                    1,
                    bloque.largo(),
                    BigDecimal.ZERO,
                    null,
                    null,
                    restanteCm,
                    restanteCm.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP),
                    false,
                    "DentIA_INVENTARIO"
            ));
        }

        BigDecimal utilizadaM = bloque.largo()
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal restanteM = restanteCm
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal aprovechamiento = origen.longitud().signum() <= 0
                ? BigDecimal.ZERO
                : bloque.largo().multiply(BigDecimal.valueOf(100))
                .divide(origen.longitud(), 2, RoundingMode.HALF_UP);

        return new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                comercial.numero(),
                comercial.codigo(),
                comercial.idPrecioPresentacion(),
                comercial.nombrePresentacion(),
                origen.idParteInventario(),
                origen.tipoParteInventario(),
                "FISICO_CON_BLOQUE_COMERCIAL",
                null,
                null,
                origen.longitud(),
                comercial.cantidadComercial(),
                comercial.unidadComercial(),
                bloque,
                bloque.cortes(),
                List.copyOf(sobrantesDentIA),
                utilizadaM,
                restanteM,
                aprovechamiento
        );
    }

    private record AsignacionFisica(
            Fisica piezaComercial,
            Fisica parteInventario,
            boolean derivadaDeCompleta
    ) {
    }


    private CalculoMedidasRespuestaDto.PlanoCorteDto
    envolverBloquesComercialesAreaEnPiezasFisicas(
            CalculoMedidasRespuestaDto.PlanoCorteDto planoComercial,
            List<Fisica> piezasDisponibles,
            CalculoMedidasRespuestaDto.LineaComercialDto lineaComercial
    ) {
        if (planoComercial == null
                || planoComercial.piezas() == null
                || planoComercial.piezas().isEmpty()) {
            throw new IllegalArgumentException(
                    "No se generaron bloques comerciales para ubicar en el inventario."
            );
        }

        List<Fisica> disponibles = new ArrayList<>(piezasDisponibles);
        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> resultado =
                new ArrayList<>();

        int numeroPlano = 1;

        for (CalculoMedidasRespuestaDto.PiezaFisicaDto comercial
                : planoComercial.piezas()) {

            if (!positivo(comercial.ancho())
                    || !positivo(comercial.alto())) {
                throw new IllegalArgumentException(
                        "El bloque comercial no tiene dimensiones físicas válidas."
                );
            }

            SeleccionPiezaArea seleccion =
                    seleccionarPiezaParaBloqueArea(
                            disponibles,
                            comercial
                    );

            if (seleccion == null) {
                throw new IllegalArgumentException(
                        "No existe una plancha, media plancha o retazo que pueda contener "
                                + "físicamente el bloque comercial cobrado."
                );
            }

            disponibles.remove(seleccion.indice());

            Fisica fisica = seleccion.fisica();

            CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                    crearBloqueComercialArea(
                            comercial,
                            seleccion.rotado()
                    );

            List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesDentIA =
                    construirSobrantesDentIAFueraBloqueArea(
                            fisica,
                            bloque
                    );

            BigDecimal areaFisicaM2 =
                    fisica.largo()
                            .multiply(fisica.ancho())
                            .divide(
                                    new BigDecimal("1000000"),
                                    6,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal areaBloqueM2 =
                    bloque.ancho()
                            .multiply(bloque.alto())
                            .divide(
                                    new BigDecimal("1000000"),
                                    6,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal areaRestanteM2 =
                    areaFisicaM2
                            .subtract(areaBloqueM2)
                            .max(BigDecimal.ZERO)
                            .setScale(6, RoundingMode.HALF_UP);

            BigDecimal aprovechamientoFisico =
                    areaFisicaM2.signum() <= 0
                            ? BigDecimal.ZERO
                            : areaBloqueM2
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    areaFisicaM2,
                                    2,
                                    RoundingMode.HALF_UP
                            );

            resultado.add(
                    new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                            numeroPlano,
                            "PLANO-" + numeroPlano,
                            lineaComercial.idPrecioPresentacion(),
                            fisica.nombrePresentacion(),
                            fisica.idParteInventario(),
                            fisica.tipoParteInventario(),
                            "FISICO_CON_BLOQUE_COMERCIAL",
                            fisica.largo(),
                            fisica.ancho(),
                            null,
                            comercial.cantidadComercial(),
                            lineaComercial.tipoCalculoVenta(),
                            bloque,
                            bloque.cortes(),
                            List.copyOf(sobrantesDentIA),
                            areaBloqueM2,
                            areaRestanteM2,
                            aprovechamientoFisico
                    )
            );

            numeroPlano++;
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                "AREA",
                planoComercial.unidad(),
                List.copyOf(resultado)
        );
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto
    envolverBloquesComercialesLongitudEnPiezasFisicas(
            CalculoMedidasRespuestaDto.PlanoCorteDto planoComercial,
            List<Fisica> piezasDisponibles,
            CalculoMedidasRespuestaDto.LineaComercialDto lineaComercial
    ) {
        if (planoComercial == null
                || planoComercial.piezas() == null
                || planoComercial.piezas().isEmpty()) {
            throw new IllegalArgumentException(
                    "No se generaron bloques comerciales lineales."
            );
        }

        List<Fisica> disponibles = new ArrayList<>(piezasDisponibles);
        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> resultado =
                new ArrayList<>();

        int numeroPlano = 1;

        for (CalculoMedidasRespuestaDto.PiezaFisicaDto comercial
                : planoComercial.piezas()) {

            if (!positivo(comercial.largo())) {
                throw new IllegalArgumentException(
                        "El bloque comercial lineal no tiene longitud válida."
                );
            }

            int indiceElegido = -1;
            Fisica elegida = null;

            for (int i = 0; i < disponibles.size(); i++) {
                Fisica candidata = disponibles.get(i);

                if (!positivo(candidata.longitud())
                        || candidata.longitud()
                        .compareTo(comercial.largo()) < 0) {
                    continue;
                }

                if (elegida == null
                        || candidata.longitud()
                        .compareTo(elegida.longitud()) < 0) {
                    elegida = candidata;
                    indiceElegido = i;
                }
            }

            if (elegida == null) {
                throw new IllegalArgumentException(
                        "No existe una pieza lineal que pueda contener la longitud cobrada."
                );
            }

            disponibles.remove(indiceElegido);

            CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                    new CalculoMedidasRespuestaDto.BloqueComercialDto(
                            "MEDIDA",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            null,
                            null,
                            comercial.largo(),
                            comercial.cantidadComercial(),
                            lineaComercial.tipoCalculoVenta(),
                            comercial.cortes(),
                            comercial.sobrantes(),
                            comercial.medidaUtilizada(),
                            comercial.medidaSobrante(),
                            comercial.aprovechamientoPorcentaje()
                    );

            BigDecimal restanteCm =
                    elegida.longitud()
                            .subtract(comercial.largo())
                            .max(BigDecimal.ZERO);

            List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesDentIA =
                    new ArrayList<>();

            if (restanteCm.signum() > 0) {
                sobrantesDentIA.add(
                        new CalculoMedidasRespuestaDto.SobranteDto(
                                1,
                                comercial.largo(),
                                BigDecimal.ZERO,
                                null,
                                null,
                                restanteCm,
                                restanteCm.divide(
                                        new BigDecimal("100"),
                                        6,
                                        RoundingMode.HALF_UP
                                ),
                                false,
                                "DentIA_INVENTARIO"
                        )
                );
            }

            BigDecimal utilizadaM =
                    comercial.largo()
                            .divide(
                                    new BigDecimal("100"),
                                    6,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal restanteM =
                    restanteCm.divide(
                            new BigDecimal("100"),
                            6,
                            RoundingMode.HALF_UP
                    );

            BigDecimal aprovechamientoFisico =
                    elegida.longitud().signum() <= 0
                            ? BigDecimal.ZERO
                            : comercial.largo()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    elegida.longitud(),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            resultado.add(
                    new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                            numeroPlano,
                            "PLANO-" + numeroPlano,
                            lineaComercial.idPrecioPresentacion(),
                            elegida.nombrePresentacion(),
                            elegida.idParteInventario(),
                            elegida.tipoParteInventario(),
                            "FISICO_CON_BLOQUE_COMERCIAL",
                            null,
                            null,
                            elegida.longitud(),
                            comercial.cantidadComercial(),
                            lineaComercial.tipoCalculoVenta(),
                            bloque,
                            bloque.cortes(),
                            List.copyOf(sobrantesDentIA),
                            utilizadaM,
                            restanteM,
                            aprovechamientoFisico
                    )
            );

            numeroPlano++;
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                "LONGITUD",
                planoComercial.unidad(),
                List.copyOf(resultado)
        );
    }

    private SeleccionPiezaArea seleccionarPiezaParaBloqueArea(
            List<Fisica> disponibles,
            CalculoMedidasRespuestaDto.PiezaFisicaDto bloque
    ) {
        SeleccionPiezaArea mejor = null;

        for (int i = 0; i < disponibles.size(); i++) {
            Fisica candidata = disponibles.get(i);

            if (!positivo(candidata.largo())
                    || !positivo(candidata.ancho())) {
                continue;
            }

            boolean normal =
                    bloque.ancho().compareTo(candidata.largo()) <= 0
                            && bloque.alto().compareTo(candidata.ancho()) <= 0;

            boolean rotado =
                    bloque.alto().compareTo(candidata.largo()) <= 0
                            && bloque.ancho().compareTo(candidata.ancho()) <= 0;

            if (!normal && !rotado) {
                continue;
            }

            BigDecimal areaPieza =
                    candidata.largo().multiply(candidata.ancho());

            boolean usarRotado =
                    !normal && rotado;

            SeleccionPiezaArea actual =
                    new SeleccionPiezaArea(
                            i,
                            candidata,
                            usarRotado,
                            areaPieza
                    );

            if (mejor == null
                    || actual.areaPieza()
                    .compareTo(mejor.areaPieza()) < 0
                    || (actual.areaPieza()
                    .compareTo(mejor.areaPieza()) == 0
                    && candidata.esRetazo()
                    && !mejor.fisica().esRetazo())) {
                mejor = actual;
            }
        }

        return mejor;
    }

    private CalculoMedidasRespuestaDto.BloqueComercialDto
    crearBloqueComercialArea(
            CalculoMedidasRespuestaDto.PiezaFisicaDto comercial,
            boolean rotado
    ) {
        if (!rotado) {
            return new CalculoMedidasRespuestaDto.BloqueComercialDto(
                    "MEDIDA",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    comercial.ancho(),
                    comercial.alto(),
                    null,
                    comercial.cantidadComercial(),
                    comercial.unidadComercial(),
                    comercial.cortes(),
                    comercial.sobrantes(),
                    comercial.medidaUtilizada(),
                    comercial.medidaSobrante(),
                    comercial.aprovechamientoPorcentaje()
            );
        }

        List<CalculoMedidasRespuestaDto.CorteUbicadoDto> cortesRotados =
                comercial.cortes() == null
                        ? List.of()
                        : comercial.cortes().stream()
                        .map(corte -> rotarCorteDentroArea(
                                corte,
                                comercial.ancho()
                        ))
                        .toList();

        List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesRotados =
                comercial.sobrantes() == null
                        ? List.of()
                        : comercial.sobrantes().stream()
                        .map(sobrante -> rotarSobranteDentroArea(
                                sobrante,
                                comercial.ancho()
                        ))
                        .toList();

        return new CalculoMedidasRespuestaDto.BloqueComercialDto(
                "MEDIDA",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                comercial.alto(),
                comercial.ancho(),
                null,
                comercial.cantidadComercial(),
                comercial.unidadComercial(),
                List.copyOf(cortesRotados),
                List.copyOf(sobrantesRotados),
                comercial.medidaUtilizada(),
                comercial.medidaSobrante(),
                comercial.aprovechamientoPorcentaje()
        );
    }

    private CalculoMedidasRespuestaDto.CorteUbicadoDto
    rotarCorteDentroArea(
            CalculoMedidasRespuestaDto.CorteUbicadoDto corte,
            BigDecimal anchoOriginal
    ) {
        BigDecimal nuevoX = corte.y();
        BigDecimal nuevoY =
                anchoOriginal
                        .subtract(corte.x().add(corte.ancho()));

        return new CalculoMedidasRespuestaDto.CorteUbicadoDto(
                corte.numero(),
                corte.codigoGrupo(),
                nuevoX,
                nuevoY,
                corte.alto(),
                corte.ancho(),
                corte.largo(),
                !corte.rotado(),
                corte.etiqueta()
        );
    }

    private CalculoMedidasRespuestaDto.SobranteDto
    rotarSobranteDentroArea(
            CalculoMedidasRespuestaDto.SobranteDto sobrante,
            BigDecimal anchoOriginal
    ) {
        BigDecimal nuevoX = sobrante.y();
        BigDecimal nuevoY =
                anchoOriginal
                        .subtract(
                                sobrante.x().add(sobrante.ancho())
                        );

        return new CalculoMedidasRespuestaDto.SobranteDto(
                sobrante.numero(),
                nuevoX,
                nuevoY,
                sobrante.alto(),
                sobrante.ancho(),
                sobrante.largo(),
                sobrante.medida(),
                sobrante.entregarCliente(),
                sobrante.clasificacion()
        );
    }

    private List<CalculoMedidasRespuestaDto.SobranteDto>
    construirSobrantesDentIAFueraBloqueArea(
            Fisica fisica,
            CalculoMedidasRespuestaDto.BloqueComercialDto bloque
    ) {
        List<CalculoMedidasRespuestaDto.SobranteDto> resultado =
                new ArrayList<>();

        int numero = 1;

        BigDecimal derecha =
                fisica.largo().subtract(bloque.ancho());

        if (derecha.signum() > 0) {
            BigDecimal area =
                    derecha.multiply(bloque.alto());

            resultado.add(
                    new CalculoMedidasRespuestaDto.SobranteDto(
                            numero++,
                            bloque.ancho(),
                            BigDecimal.ZERO,
                            derecha,
                            bloque.alto(),
                            null,
                            area.divide(
                                    new BigDecimal("1000000"),
                                    6,
                                    RoundingMode.HALF_UP
                            ),
                            false,
                            "DentIA_INVENTARIO"
                    )
            );
        }

        BigDecimal abajo =
                fisica.ancho().subtract(bloque.alto());

        if (abajo.signum() > 0) {
            BigDecimal area =
                    fisica.largo().multiply(abajo);

            resultado.add(
                    new CalculoMedidasRespuestaDto.SobranteDto(
                            numero,
                            BigDecimal.ZERO,
                            bloque.alto(),
                            fisica.largo(),
                            abajo,
                            null,
                            area.divide(
                                    new BigDecimal("1000000"),
                                    6,
                                    RoundingMode.HALF_UP
                            ),
                            false,
                            "DentIA_INVENTARIO"
                    )
            );
        }

        return List.copyOf(resultado);
    }

    private record SeleccionPiezaArea(
            int indice,
            Fisica fisica,
            boolean rotado,
            BigDecimal areaPieza
    ) {
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto
    construirPlanoResidualArea(
            CalculoMedidasRespuestaDto.CombinacionDto recomendada,
            List<CalculoMedidasRespuestaDto.CombinacionDto> candidatas,
            List<Fisica> fisicas,
            List<Corte> cortes,
            String unidad,
            boolean entregarSobrante,
            InventarioService.DisponibilidadFisicaProducto disponibilidadFisica
    ) {
        String[] partes = recomendada.codigo().split("\\|");
        if (partes.length < 5) {
            throw new IllegalArgumentException("El código de la combinación residual no es válido.");
        }

        String codigoBase = partes[1];
        int indicePiezaRetirada;
        try {
            indicePiezaRetirada = Integer.parseInt(partes[2]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El índice de la pieza residual no es válido.");
        }

        CalculoMedidasRespuestaDto.CombinacionDto combinacionBase = candidatas.stream()
                .filter(c -> Objects.equals(c.codigo(), codigoBase))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la combinación física base del cálculo residual."));

        List<Fisica> piezasBase = new ArrayList<>(expandirPiezasFisicas(combinacionBase, fisicas));
        if (indicePiezaRetirada < 0 || indicePiezaRetirada >= piezasBase.size()) {
            throw new IllegalArgumentException("La pieza física retirada no existe.");
        }

        List<Fisica> piezasInventario = construirPiezasFisicasDisponibles(fisicas, disponibilidadFisica);
        boolean tienePartesInventario = piezasInventario != null && !piezasInventario.isEmpty();

        List<AsignacionFisica> asignacionesBase = tienePartesInventario
                ? new ArrayList<>(asignarPartesInventarioACombinacion(piezasBase, piezasInventario, "AREA"))
                : new ArrayList<>();

        Fisica piezaComercialResidual = piezasBase.remove(indicePiezaRetirada);
        AsignacionFisica asignacionResidual = tienePartesInventario
                ? asignacionesBase.remove(indicePiezaRetirada)
                : null;

        CalculoMedidasRespuestaDto.LineaComercialDto lineaSuelta = recomendada.lineas().stream()
                .filter(linea -> esSueltaTipo(linea.tipoCalculoVenta())
                        && linea.numerosCorte() != null
                        && !linea.numerosCorte().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la línea comercial correspondiente a los cortes residuales."));

        Set<Integer> numerosResiduales = new LinkedHashSet<>(lineaSuelta.numerosCorte());
        List<CorteNumerado> cortesFisicos = new ArrayList<>();
        List<CorteNumerado> cortesResiduales = new ArrayList<>();

        for (int i = 0; i < cortes.size(); i++) {
            int numero = i + 1;
            CorteNumerado numerado = new CorteNumerado(numero, cortes.get(i));
            if (numerosResiduales.contains(numero)) {
                cortesResiduales.add(numerado);
            } else {
                cortesFisicos.add(numerado);
            }
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> resultado = new ArrayList<>();

        /* Presentaciones físicas: MEDIA/COMPLETA conservan su propia política. */
        if (!cortesFisicos.isEmpty() && !piezasBase.isEmpty()) {
            CalculoMedidasRespuestaDto.PlanoCorteDto planoFisicoComercial =
                    construirPlanoAreaNumerado(piezasBase, cortesFisicos, unidad, false);
            CalculoMedidasRespuestaDto.PlanoCorteDto planoFisicoMaterializado = tienePartesInventario
                    ? materializarPlanoConPartesInventario(
                            planoFisicoComercial,
                            asignacionesBase,
                            "AREA"
                    )
                    : planoFisicoComercial;
            CalculoMedidasRespuestaDto.PlanoCorteDto planoFisicoConPolitica =
                    aplicarPoliticaPresentacionComprada(
                            planoFisicoMaterializado,
                            entregarSobrante,
                            "AREA"
                    );
            resultado.addAll(planoFisicoConPolitica.piezas());
        }

        /*
         * Línea por PIE2/M2: usa exclusivamente la pieza retirada de la base,
         * evita doble cobro y clasifica sus restos como DentIA o merma.
         */
        if (!cortesResiduales.isEmpty()) {
            CalculoMedidasRespuestaDto.PlanoCorteDto planoResidualComercial =
                    construirPlanoAreaNumerado(
                            List.of(piezaComercialResidual),
                            cortesResiduales,
                            unidad,
                            false
                    );
            CalculoMedidasRespuestaDto.PlanoCorteDto planoResidualMaterializado = tienePartesInventario
                    ? materializarPlanoConPartesInventario(
                            planoResidualComercial,
                            List.of(asignacionResidual),
                            "AREA"
                    )
                    : planoResidualComercial;
            CalculoMedidasRespuestaDto.PlanoCorteDto planoResidualExacto =
                    convertirPlanoACompraPorMedidaExacta(
                            planoResidualMaterializado,
                            lineaSuelta
                    );
            resultado.addAll(planoResidualExacto.piezas());
        }

        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("La combinación mixta no generó planos físicos válidos.");
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> renumeradas = new ArrayList<>();
        for (int i = 0; i < resultado.size(); i++) {
            CalculoMedidasRespuestaDto.PiezaFisicaDto pieza = resultado.get(i);
            int numero = i + 1;
            renumeradas.add(new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                    numero,
                    "PLANO-" + numero,
                    pieza.idPrecioPresentacion(),
                    pieza.nombrePresentacion(),
                    pieza.idParteInventario(),
                    pieza.tipoParteInventario(),
                    pieza.tipoPlano(),
                    pieza.ancho(),
                    pieza.alto(),
                    pieza.largo(),
                    pieza.cantidadComercial(),
                    pieza.unidadComercial(),
                    pieza.bloqueComercial(),
                    pieza.cortes(),
                    pieza.sobrantes(),
                    pieza.medidaUtilizada(),
                    pieza.medidaSobrante(),
                    pieza.aprovechamientoPorcentaje()
            ));
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                "AREA",
                unidad,
                List.copyOf(renumeradas)
        );
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoComercialArea(
            CalculoMedidasRespuestaDto.LineaComercialDto linea,
            List<Integer> numerosCorte,
            List<Corte> cortes,
            String unidad,
            List<Fisica> fisicas
    ) {
        BigDecimal areaTotal = linea.cantidad().multiply(linea.equivalenciaUnidadBase());
        Fisica limite = fisicas.stream()
                .filter(f -> positivo(f.largo) && positivo(f.ancho))
                .max(Comparator.comparing(Fisica::capacidadCobro))
                .orElse(null);
        if (limite == null || areaTotal.compareTo(limite.capacidadCobro) <= 0) {
            return construirPlanoComercialAreaUnico(linea, numerosCorte, cortes, unidad);
        }

        int cantidadPlanos = areaTotal.divide(limite.capacidadCobro, 0, RoundingMode.CEILING).intValue();
        if (cantidadPlanos > 100) {
            throw new IllegalArgumentException("La cantidad comercial requiere más de 100 planos.");
        }
        List<Fisica> referencias = new ArrayList<>();
        for (int i = 0; i < cantidadPlanos; i++) referencias.add(limite);
        List<CorteNumerado> numerados = numerosCorte.stream()
                .filter(numero -> numero != null && numero > 0 && numero <= cortes.size())
                .map(numero -> new CorteNumerado(numero, cortes.get(numero - 1)))
                .toList();
        CalculoMedidasRespuestaDto.PlanoCorteDto fisico = construirPlanoAreaNumerado(
                referencias, numerados, unidad, false);
        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> grupos = new ArrayList<>(fisico.piezas());
        grupos.sort(Comparator.comparing(CalculoMedidasRespuestaDto.PiezaFisicaDto::medidaUtilizada).reversed()
                .thenComparingInt(CalculoMedidasRespuestaDto.PiezaFisicaDto::numero));

        List<BigDecimal> areasCobradas = grupos.stream()
                .map(CalculoMedidasRespuestaDto.PiezaFisicaDto::medidaUtilizada)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        BigDecimal areaUsada = areasCobradas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal porDistribuir = areaTotal.subtract(areaUsada);
        for (int i = 0; i < areasCobradas.size() && porDistribuir.signum() > 0; i++) {
            BigDecimal capacidadLibre = limite.capacidadCobro.subtract(areasCobradas.get(i));
            BigDecimal adicional = capacidadLibre.min(porDistribuir);
            areasCobradas.set(i, areasCobradas.get(i).add(adicional));
            porDistribuir = porDistribuir.subtract(adicional);
        }
        if (porDistribuir.signum() > 0) {
            throw new IllegalArgumentException("La cantidad comercial supera la capacidad máxima configurada.");
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> planos = new ArrayList<>();
        for (int i = 0; i < grupos.size(); i++) {
            int numero = i + 1;
            CalculoMedidasRespuestaDto.PiezaFisicaDto grupo = grupos.get(i);
            BigDecimal areaCobrada = areasCobradas.get(i);
            BigDecimal cantidadComercial = areaCobrada.divide(
                    linea.equivalenciaUnidadBase(), 12, RoundingMode.HALF_UP);
            var lineaPlano = new CalculoMedidasRespuestaDto.LineaComercialDto(
                    linea.idPrecioPresentacion(), linea.nombrePresentacion(), linea.tipoCalculoVenta(),
                    cantidadComercial, linea.precioUnitario(), linea.precioUnitario().multiply(cantidadComercial),
                    linea.factorConversionStock(), linea.lineaPrincipal(),
                    grupo.cortes().stream().map(CalculoMedidasRespuestaDto.CorteUbicadoDto::numero).toList(),
                    linea.equivalenciaUnidadBase());
            CalculoMedidasRespuestaDto.PiezaFisicaDto geometria;
            if (lineaPlano.numerosCorte().isEmpty()) {
                BigDecimal alto = areaCobrada.multiply(new BigDecimal("1000000"))
                        .divide(limite.largo, 12, RoundingMode.CEILING);
                geometria = new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                        1, "PLANO-1", linea.idPrecioPresentacion(), linea.nombrePresentacion(),
                        "COMERCIAL_MEDIDA", limite.largo, alto, null, cantidadComercial,
                        linea.tipoCalculoVenta(), List.of(), List.of(), BigDecimal.ZERO,
                        areaCobrada.setScale(6, RoundingMode.HALF_UP), BigDecimal.ZERO);
            } else {
                geometria = construirPlanoComercialAreaUnico(
                        lineaPlano, lineaPlano.numerosCorte(), cortes, unidad).piezas().get(0);
            }
            boolean dentro = (geometria.ancho().compareTo(limite.largo) <= 0
                    && geometria.alto().compareTo(limite.ancho) <= 0)
                    || (geometria.ancho().compareTo(limite.ancho) <= 0
                    && geometria.alto().compareTo(limite.largo) <= 0);
            if (!dentro) {
                throw new IllegalArgumentException(
                        "Un bloque comercial no cabe dentro de las dimensiones máximas configuradas.");
            }
            planos.add(new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                    numero, "PLANO-" + numero, linea.idPrecioPresentacion(), linea.nombrePresentacion(),
                    "COMERCIAL_MEDIDA", geometria.ancho(), geometria.alto(),
                    null, cantidadComercial, linea.tipoCalculoVenta(),
                    geometria.cortes(), geometria.sobrantes(), geometria.medidaUtilizada(),
                    geometria.medidaSobrante(), geometria.aprovechamientoPorcentaje()));
        }
        return new CalculoMedidasRespuestaDto.PlanoCorteDto("AREA", unidad, List.copyOf(planos));
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoComercialAreaUnico(
            CalculoMedidasRespuestaDto.LineaComercialDto linea,
            List<Integer> numerosCorte,
            List<Corte> cortes,
            String unidad
    ) {
        BigDecimal areaMm2 = BigDecimal.ZERO;
        List<CorteNumerado> seleccionados = new ArrayList<>();
        for (Integer numero : numerosCorte) {
            if (numero == null || numero <= 0 || numero > cortes.size()) continue;
            Corte corte = cortes.get(numero - 1);
            seleccionados.add(new CorteNumerado(numero, corte));
            areaMm2 = areaMm2.add(corte.ancho.multiply(corte.alto));
        }

        BigDecimal areaRealM2 = areaMm2.divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP);
        String tipoComercial = normalizar(linea.tipoCalculoVenta());
        if (!positivo(linea.equivalenciaUnidadBase())) {
            throw new IllegalArgumentException("La presentación por medida no tiene una equivalencia configurada.");
        }
        BigDecimal areaCobradaM2 = linea.cantidad().multiply(linea.equivalenciaUnidadBase());
        if (areaCobradaM2.compareTo(areaRealM2) < 0) {
            throw new IllegalArgumentException("La superficie comercial cobrada es menor que el área real de los cortes.");
        }
        SuperficieComercial superficie = empaquetarSuperficieComercial(
                seleccionados, areaCobradaM2.multiply(new BigDecimal("1000000")));
        List<CalculoMedidasRespuestaDto.CorteUbicadoDto> ubicados = superficie.cortes.stream()
                .map(corte -> new CalculoMedidasRespuestaDto.CorteUbicadoDto(
                        corte.numero, "CORTE-" + corte.numero,
                        corte.ubicacion.x, corte.ubicacion.y,
                        corte.ubicacion.w, corte.ubicacion.h, null, corte.ubicacion.rotado,
                        corte.original.ancho.stripTrailingZeros().toPlainString() + " × "
                                + corte.original.alto.stripTrailingZeros().toPlainString() + " "
                                + unidad.toLowerCase(Locale.ROOT)))
                .toList();
        BigDecimal sobranteComercial = areaCobradaM2.subtract(areaRealM2)
                .max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
        BigDecimal aprovechamiento = areaCobradaM2.signum() <= 0 ? BigDecimal.ZERO
                : areaRealM2.multiply(BigDecimal.valueOf(100))
                .divide(areaCobradaM2, 2, RoundingMode.HALF_UP);

        List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesCliente = new ArrayList<>();
        int numeroSobrante = 1;
        for (Rect libre : superficie.libres()) {
            BigDecimal areaLibreMm2 = libre.w().multiply(libre.h());
            if (areaLibreMm2.signum() <= 0) {
                continue;
            }
            sobrantesCliente.add(new CalculoMedidasRespuestaDto.SobranteDto(
                    numeroSobrante++,
                    libre.x(),
                    libre.y(),
                    libre.w(),
                    libre.h(),
                    null,
                    areaLibreMm2.divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP),
                    false,
                    clasificarSobranteArea(areaLibreMm2.divide(
                            new BigDecimal("1000000"), 6, RoundingMode.HALF_UP))
            ));
        }

        var pieza = new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                1, "PLANO-1", linea.idPrecioPresentacion(), linea.nombrePresentacion(),
                "COMERCIAL_MEDIDA", superficie.ancho, superficie.alto, null,
                linea.cantidad(), linea.tipoCalculoVenta(), List.copyOf(ubicados),
                List.copyOf(sobrantesCliente),
                areaRealM2.setScale(6, RoundingMode.HALF_UP), sobranteComercial, aprovechamiento);
        return new CalculoMedidasRespuestaDto.PlanoCorteDto("AREA", unidad, List.of(pieza));
    }

    private SuperficieComercial empaquetarSuperficieComercial(
            List<CorteNumerado> cortes, BigDecimal areaCobradaMm2) {
        if (cortes.isEmpty() || areaCobradaMm2.signum() <= 0) {
            throw new IllegalArgumentException("No hay una superficie comercial válida para dibujar.");
        }
        LinkedHashSet<BigDecimal> anchos = new LinkedHashSet<>();
        BigDecimal sumaAnchos = BigDecimal.ZERO;
        BigDecimal sumaAltos = BigDecimal.ZERO;
        for (CorteNumerado corte : cortes) {
            anchos.add(corte.corte.ancho);
            anchos.add(corte.corte.alto);
            sumaAnchos = sumaAnchos.add(corte.corte.ancho);
            sumaAltos = sumaAltos.add(corte.corte.alto);
        }
        anchos.add(sumaAnchos);
        anchos.add(sumaAltos);
        anchos.add(BigDecimal.valueOf(Math.sqrt(areaCobradaMm2.doubleValue())));

        List<List<CorteNumerado>> ordenes = new ArrayList<>();
        ordenes.add(cortes.stream().sorted(Comparator
                .comparing((CorteNumerado c) -> c.corte.ancho.multiply(c.corte.alto)).reversed()
                .thenComparingInt(CorteNumerado::numero)).toList());
        if (!ordenes.get(0).equals(cortes)) ordenes.add(List.copyOf(cortes));

        SuperficieComercial mejor = null;
        for (BigDecimal ancho : anchos) {
            if (!positivo(ancho)) continue;
            BigDecimal alto = areaCobradaMm2.divide(ancho, 12, RoundingMode.CEILING);
            for (List<CorteNumerado> orden : ordenes) {
                RectBinComercial bin = new RectBinComercial(ancho, alto);
                boolean completo = true;
                for (CorteNumerado corte : orden) {
                    Ubicacion ubicacion = bin.colocar(corte.corte);
                    if (ubicacion == null) {
                        completo = false;
                        break;
                    }
                    bin.cortes.add(new CortePlanoArea(corte.numero, corte.corte, ubicacion));
                }
                if (!completo) continue;
                SuperficieComercial candidata = new SuperficieComercial(
                        ancho,
                        alto,
                        List.copyOf(bin.cortes),
                        List.copyOf(bin.libres)
                );
                if (mejor == null || candidata.desbalance().compareTo(mejor.desbalance()) < 0
                        || (candidata.desbalance().compareTo(mejor.desbalance()) == 0
                        && candidata.ancho.compareTo(mejor.ancho) < 0)) {
                    mejor = candidata;
                }
            }
        }
        if (mejor == null) {
            throw new IllegalArgumentException("No fue posible construir una superficie comercial que contenga los cortes.");
        }
        return mejor;
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoComercialLongitud(
            CalculoMedidasRespuestaDto.LineaComercialDto linea,
            List<Integer> numerosCorte,
            List<Corte> cortes,
            String unidad
    ) {
        if (!positivo(linea.equivalenciaUnidadBase())) {
            throw new IllegalArgumentException("La presentación lineal no tiene una equivalencia configurada.");
        }
        BigDecimal longitudCobradaCm = linea.cantidad()
                .multiply(linea.equivalenciaUnidadBase())
                .multiply(new BigDecimal("100"));
        BigDecimal utilizadaCm = BigDecimal.ZERO;
        List<CalculoMedidasRespuestaDto.CorteUbicadoDto> ubicados = new ArrayList<>();
        for (Integer numero : numerosCorte) {
            if (numero == null || numero <= 0 || numero > cortes.size()) continue;
            Corte corte = cortes.get(numero - 1);
            if (utilizadaCm.add(corte.longitud).compareTo(longitudCobradaCm) > 0) {
                throw new IllegalArgumentException("La longitud comercial cobrada no contiene todos los cortes.");
            }
            ubicados.add(new CalculoMedidasRespuestaDto.CorteUbicadoDto(
                    numero, "CORTE-" + numero, utilizadaCm, BigDecimal.ZERO,
                    null, null, corte.longitud, false,
                    corte.longitud.stripTrailingZeros().toPlainString() + " "
                            + unidad.toLowerCase(Locale.ROOT)));
            utilizadaCm = utilizadaCm.add(corte.longitud);
        }
        BigDecimal utilizadaM = utilizadaCm.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal diferencia = longitudCobradaCm.subtract(utilizadaCm).max(BigDecimal.ZERO)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal aprovechamiento = longitudCobradaCm.signum() <= 0 ? BigDecimal.ZERO
                : utilizadaCm.multiply(BigDecimal.valueOf(100))
                .divide(longitudCobradaCm, 2, RoundingMode.HALF_UP);
        List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesCliente = new ArrayList<>();
        if (longitudCobradaCm.compareTo(utilizadaCm) > 0) {
            sobrantesCliente.add(new CalculoMedidasRespuestaDto.SobranteDto(
                    1,
                    utilizadaCm,
                    BigDecimal.ZERO,
                    null,
                    null,
                    longitudCobradaCm.subtract(utilizadaCm),
                    diferencia,
                    false,
                    "DentIA_INVENTARIO"
            ));
        }

        var pieza = new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                1, "PLANO-1", linea.idPrecioPresentacion(), linea.nombrePresentacion(),
                "COMERCIAL_MEDIDA", null, null, longitudCobradaCm,
                linea.cantidad(), linea.tipoCalculoVenta(), List.copyOf(ubicados),
                List.copyOf(sobrantesCliente),
                utilizadaM, diferencia, aprovechamiento);
        return new CalculoMedidasRespuestaDto.PlanoCorteDto("LONGITUD", unidad, List.of(pieza));
    }

    private List<Fisica> expandirPiezasFisicas(
            CalculoMedidasRespuestaDto.CombinacionDto combinacion,
            List<Fisica> fisicas
    ) {
        Map<Integer, Fisica> porPresentacion = new HashMap<>();

        for (Fisica fisica : fisicas) {
            porPresentacion.put(
                    fisica.presentacion.getIdPrecioPresentacion(),
                    fisica
            );
        }

        List<Fisica> resultado = new ArrayList<>();

        for (CalculoMedidasRespuestaDto.LineaComercialDto linea
                : combinacion.lineas()) {

            Fisica fisica = porPresentacion.get(
                    linea.idPrecioPresentacion()
            );

            if (fisica == null) {
                continue;
            }

            int cantidad;

            try {
                cantidad = linea.cantidad().intValueExact();
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException(
                        "La cantidad de la presentación física debe ser entera."
                );
            }

            for (int i = 0; i < cantidad; i++) {
                resultado.add(fisica);
            }
        }

        return resultado;
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoArea(
            List<Fisica> piezasSeleccionadas,
            List<Corte> cortes,
            String unidad,
            boolean entregarSobrante
    ) {
        List<CorteNumerado> numerados = new ArrayList<>();
        for (int i = 0; i < cortes.size(); i++) {
            numerados.add(new CorteNumerado(i + 1, cortes.get(i)));
        }
        return construirPlanoAreaNumerado(
                piezasSeleccionadas, numerados, unidad, entregarSobrante);
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoAreaNumerado(
            List<Fisica> piezasSeleccionadas,
            List<CorteNumerado> cortesNumerados,
            String unidad,
            boolean entregarSobrante
    ) {
        List<RectBinPlano> planchas = null;
        for (List<CorteNumerado> orden : ordenesAreaNumerados(cortesNumerados)) {
            List<RectBinPlano> intento = new ArrayList<>();
            for (int i = 0; i < piezasSeleccionadas.size(); i++) {
                intento.add(new RectBinPlano(i + 1, piezasSeleccionadas.get(i)));
            }
            boolean completo = true;
            for (CorteNumerado corteNumerado : orden) {
                boolean colocado = false;
                for (RectBinPlano plancha : intento) {
                    Ubicacion ubicacion = plancha.colocar(corteNumerado.corte);
                    if (ubicacion == null) continue;
                    plancha.cortes.add(new CortePlanoArea(
                            corteNumerado.numero, corteNumerado.corte, ubicacion));
                    colocado = true;
                    break;
                }
                if (!colocado) {
                    completo = false;
                    break;
                }
            }
            if (completo) {
                planchas = intento;
                break;
            }
        }
        if (planchas == null) {
            throw new IllegalArgumentException("No fue posible reconstruir el plano físico de los cortes.");
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezas =
                new ArrayList<>();

        for (RectBinPlano plancha : planchas) {
            List<CalculoMedidasRespuestaDto.CorteUbicadoDto> cortesDto =
                    new ArrayList<>();

            BigDecimal areaUtilizadaMm2 = BigDecimal.ZERO;

            for (CortePlanoArea corte : plancha.cortes) {
                areaUtilizadaMm2 = areaUtilizadaMm2.add(
                        corte.original.ancho.multiply(corte.original.alto)
                );

                cortesDto.add(
                        new CalculoMedidasRespuestaDto.CorteUbicadoDto(
                                corte.numero,
                                "CORTE-" + corte.numero,
                                corte.ubicacion.x,
                                corte.ubicacion.y,
                                corte.ubicacion.w,
                                corte.ubicacion.h,
                                null,
                                corte.ubicacion.rotado,
                                corte.original.ancho.stripTrailingZeros().toPlainString()
                                        + " × "
                                        + corte.original.alto.stripTrailingZeros().toPlainString()
                                        + " "
                                        + unidad.toLowerCase(Locale.ROOT)
                        )
                );
            }

            List<CalculoMedidasRespuestaDto.SobranteDto> sobrantesDto =
                    new ArrayList<>();

            BigDecimal areaSobranteMm2 = BigDecimal.ZERO;
            int numeroSobrante = 1;

            for (Rect libre : plancha.libres) {
                BigDecimal area = libre.w.multiply(libre.h);

                if (area.signum() <= 0) {
                    continue;
                }

                areaSobranteMm2 = areaSobranteMm2.add(area);

                sobrantesDto.add(
                        new CalculoMedidasRespuestaDto.SobranteDto(
                                numeroSobrante++,
                                libre.x,
                                libre.y,
                                libre.w,
                                libre.h,
                                null,
                                area.divide(
                                        new BigDecimal("1000000"),
                                        6,
                                        RoundingMode.HALF_UP
                                ),
                                false,
                                clasificarSobranteArea(area.divide(
                                        new BigDecimal("1000000"),
                                        6,
                                        RoundingMode.HALF_UP
                                ))
                        )
                );
            }

            BigDecimal areaTotalMm2 =
                    plancha.fisica.largo.multiply(plancha.fisica.ancho);

            BigDecimal aprovechamiento =
                    areaTotalMm2.signum() == 0
                            ? BigDecimal.ZERO
                            : areaUtilizadaMm2
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    areaTotalMm2,
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal areaUtilizadaM2 =
                    areaUtilizadaMm2.divide(
                            new BigDecimal("1000000"),
                            6,
                            RoundingMode.HALF_UP
                    );

            BigDecimal areaSobranteM2 =
                    areaSobranteMm2.divide(
                            new BigDecimal("1000000"),
                            6,
                            RoundingMode.HALF_UP
                    );

            String tipoBloque =
                    esMediaPlancha(plancha.fisica.presentacion())
                            ? "MEDIA"
                            : esPresentacionFisicaCompleta(
                            plancha.fisica.presentacion()
                    )
                            ? "COMPLETA"
                            : "PRESENTACION";

            CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                    new CalculoMedidasRespuestaDto.BloqueComercialDto(
                            tipoBloque,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            plancha.fisica.largo,
                            plancha.fisica.ancho,
                            null,
                            BigDecimal.ONE,
                            plancha.fisica.presentacion() == null
                                    ? tipoBloque
                                    : plancha.fisica.presentacion()
                                    .getTipoCalculoVenta(),
                            List.copyOf(cortesDto),
                            List.copyOf(sobrantesDto),
                            areaUtilizadaM2,
                            areaSobranteM2,
                            aprovechamiento
                    );

            piezas.add(
                    new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                            plancha.numero,
                            "PLANO-" + plancha.numero,
                            plancha.fisica.idPrecioPresentacion(),
                            plancha.fisica.nombrePresentacion(),
                            plancha.fisica.idParteInventario(),
                            plancha.fisica.tipoParteInventario(),
                            "FISICO",
                            plancha.fisica.largo,
                            plancha.fisica.ancho,
                            null,
                            BigDecimal.ONE,
                            bloque.unidadComercial(),
                            bloque,
                            List.copyOf(cortesDto),
                            List.of(),
                            areaUtilizadaM2,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(100)
                    )
            );
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                "AREA",
                unidad,
                piezas
        );
    }

    private CalculoMedidasRespuestaDto.PlanoCorteDto construirPlanoLongitud(
            List<Fisica> piezasSeleccionadas,
            List<Corte> cortes,
            String unidad,
            boolean entregarSobrante
    ) {
        List<BarraPlano> barras = new ArrayList<>();

        for (int i = 0; i < piezasSeleccionadas.size(); i++) {
            barras.add(
                    new BarraPlano(
                            i + 1,
                            piezasSeleccionadas.get(i)
                    )
            );
        }

        List<CorteNumerado> cortesOrdenados = new ArrayList<>();

        for (int i = 0; i < cortes.size(); i++) {
            cortesOrdenados.add(
                    new CorteNumerado(i + 1, cortes.get(i))
            );
        }

        cortesOrdenados.sort(
                Comparator
                        .comparing(
                                (CorteNumerado c) -> c.corte.longitud
                        )
                        .reversed()
        );

        for (CorteNumerado corteNumerado : cortesOrdenados) {
            BarraPlano mejorBarra = null;
            BigDecimal menorResto = null;

            for (BarraPlano barra : barras) {
                if (barra.restante.compareTo(
                        corteNumerado.corte.longitud
                ) < 0) {
                    continue;
                }

                BigDecimal resto =
                        barra.restante.subtract(
                                corteNumerado.corte.longitud
                        );

                if (menorResto == null
                        || resto.compareTo(menorResto) < 0) {
                    mejorBarra = barra;
                    menorResto = resto;
                }
            }

            if (mejorBarra == null) {
                throw new IllegalArgumentException(
                        "No fue posible reconstruir el plano lineal."
                );
            }

            BigDecimal inicio =
                    mejorBarra.fisica.longitud.subtract(
                            mejorBarra.restante
                    );

            mejorBarra.cortes.add(
                    new CortePlanoLongitud(
                            corteNumerado.numero,
                            inicio,
                            corteNumerado.corte.longitud
                    )
            );

            mejorBarra.restante =
                    mejorBarra.restante.subtract(
                            corteNumerado.corte.longitud
                    );
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezas =
                new ArrayList<>();

        for (BarraPlano barra : barras) {
            List<CalculoMedidasRespuestaDto.CorteUbicadoDto> cortesDto =
                    new ArrayList<>();

            BigDecimal utilizado =
                    barra.fisica.longitud.subtract(barra.restante);

            for (CortePlanoLongitud corte : barra.cortes) {
                cortesDto.add(
                        new CalculoMedidasRespuestaDto.CorteUbicadoDto(
                                corte.numero,
                                "CORTE-" + corte.numero,
                                corte.inicio,
                                BigDecimal.ZERO,
                                null,
                                null,
                                corte.longitud,
                                false,
                                corte.longitud
                                        .stripTrailingZeros()
                                        .toPlainString()
                                        + " "
                                        + unidad.toLowerCase(Locale.ROOT)
                        )
                );
            }

            List<CalculoMedidasRespuestaDto.SobranteDto> sobrantes =
                    new ArrayList<>();

            if (barra.restante.signum() > 0) {
                sobrantes.add(
                        new CalculoMedidasRespuestaDto.SobranteDto(
                                1,
                                utilizado,
                                BigDecimal.ZERO,
                                null,
                                null,
                                barra.restante,
                                barra.restante.divide(
                                        new BigDecimal("100"),
                                        6,
                                        RoundingMode.HALF_UP
                                ),
                                false,
                                "DentIA_INVENTARIO"
                        )
                );
            }

            BigDecimal aprovechamiento =
                    barra.fisica.longitud.signum() == 0
                            ? BigDecimal.ZERO
                            : utilizado
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    barra.fisica.longitud,
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal utilizadoM =
                    utilizado.divide(
                            new BigDecimal("100"),
                            6,
                            RoundingMode.HALF_UP
                    );

            BigDecimal sobranteM =
                    barra.restante.divide(
                            new BigDecimal("100"),
                            6,
                            RoundingMode.HALF_UP
                    );

            String tipoBloque =
                    esMediaPlancha(barra.fisica.presentacion())
                            ? "MEDIA"
                            : esPresentacionFisicaCompleta(
                            barra.fisica.presentacion()
                    )
                            ? "COMPLETA"
                            : "PRESENTACION";

            CalculoMedidasRespuestaDto.BloqueComercialDto bloque =
                    new CalculoMedidasRespuestaDto.BloqueComercialDto(
                            tipoBloque,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            null,
                            null,
                            barra.fisica.longitud,
                            BigDecimal.ONE,
                            barra.fisica.presentacion() == null
                                    ? tipoBloque
                                    : barra.fisica.presentacion()
                                    .getTipoCalculoVenta(),
                            List.copyOf(cortesDto),
                            List.copyOf(sobrantes),
                            utilizadoM,
                            sobranteM,
                            aprovechamiento
                    );

            piezas.add(
                    new CalculoMedidasRespuestaDto.PiezaFisicaDto(
                            barra.numero,
                            "PLANO-" + barra.numero,
                            barra.fisica.idPrecioPresentacion(),
                            barra.fisica.nombrePresentacion(),
                            barra.fisica.idParteInventario(),
                            barra.fisica.tipoParteInventario(),
                            "FISICO",
                            null,
                            null,
                            barra.fisica.longitud,
                            BigDecimal.ONE,
                            bloque.unidadComercial(),
                            bloque,
                            List.copyOf(cortesDto),
                            List.of(),
                            utilizadoM,
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(100)
                    )
            );
        }

        return new CalculoMedidasRespuestaDto.PlanoCorteDto(
                "LONGITUD",
                unidad,
                piezas
        );
    }

    private List<Fisica> construirFisicas(List<ProductoPrecioPresentacion> activas, Producto producto, String tipo,
                                          String unidad, List<Corte> cortes, List<String> advertencias) {
        List<Fisica> out = new ArrayList<>();
        for (ProductoPrecioPresentacion p : activas) {
            if (esSuelta(p)) continue;
            BigDecimal largo;
            BigDecimal ancho;
            BigDecimal longitud;

            boolean esPresentacionCompleta =
                    esPresentacionFisicaCompleta(p);

            if (tipo.equals("AREA")) {

                if (positivo(p.getLargoBase()) && positivo(p.getAnchoBase())) {

                    largo = convertirBase(
                            p.getLargoBase(),
                            tipo,
                            unidad
                    );

                    ancho = convertirBase(
                            p.getAnchoBase(),
                            tipo,
                            unidad
                    );

                } else if (esPresentacionCompleta
                        && positivo(producto.getLargoBase())
                        && positivo(producto.getAnchoBase())) {

                    largo = convertirBase(
                            producto.getLargoBase(),
                            tipo,
                            unidad
                    );

                    ancho = convertirBase(
                            producto.getAnchoBase(),
                            tipo,
                            unidad
                    );

                } else if (esMediaPlancha(p)
                        && positivo(producto.getLargoBase())
                        && positivo(producto.getAnchoBase())) {

                    BigDecimal largoCompleto = convertirBase(
                            producto.getLargoBase(),
                            tipo,
                            unidad
                    );

                    BigDecimal anchoCompleto = convertirBase(
                            producto.getAnchoBase(),
                            tipo,
                            unidad
                    );

                    BigDecimal largoHorizontal = largoCompleto;
                    BigDecimal anchoHorizontal = anchoCompleto
                            .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);

                    BigDecimal largoVertical = largoCompleto
                            .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);
                    BigDecimal anchoVertical = anchoCompleto;

                    int cortesHorizontal = contarCortesQueCaben(
                            cortes,
                            largoHorizontal,
                            anchoHorizontal
                    );

                    int cortesVertical = contarCortesQueCaben(
                            cortes,
                            largoVertical,
                            anchoVertical
                    );

                    if (cortesHorizontal <= 0 && cortesVertical <= 0) {
                        advertencias.add(
                                "Ningún corte individual cabe en "
                                        + p.getNombrePresentacion()
                                        + "."
                        );
                    }

                    if (cortesHorizontal >= cortesVertical) {
                        largo = largoHorizontal;
                        ancho = anchoHorizontal;
                    } else {
                        largo = largoVertical;
                        ancho = anchoVertical;
                    }

                } else {

                    advertencias.add(
                            "Se excluyó "
                                    + p.getNombrePresentacion()
                                    + " porque no tiene largo y ancho propios configurados."
                    );

                    continue;
                }

                longitud = BigDecimal.ZERO;

            } else {

                if (positivo(p.getMedidaTotalBase())) {

                    longitud = convertirBase(
                            p.getMedidaTotalBase(),
                            tipo,
                            unidad
                    );

                } else if (esPresentacionCompleta
                        && positivo(producto.getMedidaTotalBase())) {

                    longitud = convertirBase(
                            producto.getMedidaTotalBase(),
                            tipo,
                            unidad
                    );

                } else {

                    advertencias.add(
                            "Se excluyó "
                                    + p.getNombrePresentacion()
                                    + " porque no tiene longitud física propia configurada."
                    );

                    continue;
                }

                largo = BigDecimal.ZERO;
                ancho = BigDecimal.ZERO;
            }
            boolean valida = tipo.equals("AREA") ? positivo(largo) && positivo(ancho) : positivo(longitud);
            if (!valida) {
                advertencias.add("Se excluyó " + p.getNombrePresentacion() + " porque no tiene dimensiones físicas válidas.");
                continue;
            }
            BigDecimal capacidad = tipo.equals("AREA")
                    ? largo.multiply(ancho).divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP)
                    : longitud.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
            out.add(new Fisica(p, largo, ancho, longitud, capacidad,
                    positivo(p.getFactorConversionStock()) ? p.getFactorConversionStock() : BigDecimal.ONE));
        }
        out.sort(Comparator.comparing(f -> f.presentacion.getPrecioVenta()));
        return out;
    }

    private List<Corte> expandirCortes(CalculoMedidasSolicitudDto solicitud, String tipo) {
        List<Corte> out = new ArrayList<>();
        for (var c : solicitud.cortes()) {
            if (tipo.equals("AREA")) {
                if (!positivo(c.ancho()) || !positivo(c.alto())) throw new IllegalArgumentException("Cada corte de área debe tener ancho y alto mayores que cero.");
                for (int i = 0; i < c.cantidad(); i++) out.add(new Corte(c.ancho(), c.alto(), BigDecimal.ZERO));
            } else {
                if (!positivo(c.longitud())) throw new IllegalArgumentException("Cada corte lineal debe tener longitud mayor que cero.");
                for (int i = 0; i < c.cantidad(); i++) out.add(new Corte(BigDecimal.ZERO, BigDecimal.ZERO, c.longitud()));
            }
            if (out.size() > 10000) throw new IllegalArgumentException("Se excedió el máximo de cortes individuales.");
        }
        return out;
    }

    private BigDecimal calcularMedidaTotal(List<Corte> cortes, String tipo) {
        if (tipo.equals("AREA")) return cortes.stream().map(c -> c.ancho.multiply(c.alto).divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        return cortes.stream().map(c -> c.longitud.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Comparator<CalculoMedidasRespuestaDto.CombinacionDto> comparador() {
        return Comparator.comparing(CalculoMedidasRespuestaDto.CombinacionDto::precioTotal)
                .thenComparing(CalculoMedidasRespuestaDto.CombinacionDto::materialAdquirido)
                .thenComparingInt(CalculoMedidasRespuestaDto.CombinacionDto::piezasFisicas)
                .thenComparingInt(c -> c.lineas().size())
                .thenComparing(this::claveComercial)
                .thenComparing(c -> Objects.toString(c.codigo(), ""));
    }

    private List<CalculoMedidasRespuestaDto.CombinacionDto> deduplicar(List<CalculoMedidasRespuestaDto.CombinacionDto> in) {
        Map<String, CalculoMedidasRespuestaDto.CombinacionDto> map = new LinkedHashMap<>();
        in.stream().sorted(comparador()).forEach(c -> map.putIfAbsent(claveComercial(c), c));
        return new ArrayList<>(map.values());
    }

    private String claveComercial(CalculoMedidasRespuestaDto.CombinacionDto combinacion) {
        String lineas = combinacion.lineas().stream()
                .map(linea -> Objects.toString(linea.idPrecioPresentacion(), "") + ":"
                        + decimalClave(linea.cantidad()) + ":"
                        + decimalClave(linea.precioUnitario()) + ":"
                        + decimalClave(linea.subtotal()))
                .sorted()
                .collect(java.util.stream.Collectors.joining("|"));
        return lineas + "#" + decimalClave(combinacion.precioTotal());
    }

    private String decimalClave(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    private BigDecimal cantidadSuelta(BigDecimal medidaTotal,
                                      ProductoPrecioPresentacion presentacion,
                                      BigDecimal equivalenciaUnidadBase) {
        if (!positivo(equivalenciaUnidadBase)) {
            throw new IllegalArgumentException("La presentación por medida no tiene una equivalencia válida.");
        }
        BigDecimal requerida = medidaTotal.divide(equivalenciaUnidadBase, 12, RoundingMode.CEILING);
        if (normalizar(presentacion.getTipoCalculoVenta()).equals("PIE2")) {
            return requerida.setScale(0, RoundingMode.CEILING);
        }
        return requerida.setScale(6, RoundingMode.CEILING);
    }

    private BigDecimal equivalenciaUnidadBase(ProductoPrecioPresentacion presentacion,
                                              Producto producto,
                                              String tipoMedida) {
        BigDecimal base;
        if (tipoMedida.equals("AREA")) {
            if (!positivo(producto.getLargoBase()) || !positivo(producto.getAnchoBase())) {
                throw new IllegalArgumentException("El producto no tiene dimensiones base para convertir la presentación por área.");
            }
            base = producto.getLargoBase().multiply(producto.getAnchoBase());
        } else {
            if (!positivo(producto.getMedidaTotalBase())) {
                throw new IllegalArgumentException("El producto no tiene longitud base para convertir la presentación lineal.");
            }
            base = producto.getMedidaTotalBase();
        }
        return base.multiply(presentacion.getFactorConversionStock())
                .setScale(12, RoundingMode.HALF_UP);
    }

    private BigDecimal factorStockPorUnidadComercial(ProductoPrecioPresentacion presentacion) {
        return presentacion.getFactorConversionStock();
    }

    private boolean esSuelta(ProductoPrecioPresentacion p) {
        return esSueltaTipo(p.getTipoCalculoVenta());
    }

    private boolean esSueltaTipo(String tipoCalculoVenta) {
        String t = normalizar(tipoCalculoVenta);
        return t.equals("PIE2") || t.equals("M2") || t.equals("METRO_CUADRADO")
                || t.equals("AREA") || t.equals("METRO_LINEAL") || t.equals("LONGITUD");
    }

    private boolean esSueltaCompatible(ProductoPrecioPresentacion p, String tipoMedida) {
        String calculo = normalizar(p.getTipoCalculoVenta());
        if (tipoMedida.equals("AREA")) {
            return calculo.equals("PIE2") || calculo.equals("M2")
                    || calculo.equals("METRO_CUADRADO") || calculo.equals("AREA");
        }
        return calculo.equals("METRO_LINEAL") || calculo.equals("LONGITUD");
    }

    private boolean esPresentacionFisicaCompleta(
            ProductoPrecioPresentacion p
    ) {
        if (p == null) {
            return false;
        }

        String nombre = normalizar(
                p.getNombrePresentacion()
        );

        String tipoCalculo = normalizar(
                p.getTipoCalculoVenta()
        );

        return nombre.contains("PLANCHA COMPLETA")
                || nombre.equals("PLANCHA")
                || nombre.contains("VARILLA COMPLETA")
                || nombre.equals("VARILLA")
                || nombre.contains("ROLLO COMPLETO")
                || tipoCalculo.equals("COMPLETA");
    }

    private boolean esMediaPlancha(
            ProductoPrecioPresentacion p
    ) {
        if (p == null) {
            return false;
        }

        String tipoCalculo = normalizar(
                p.getTipoCalculoVenta()
        );

        String nombre = normalizar(
                p.getNombrePresentacion()
        );

        return tipoCalculo.equals("MEDIA")
                || tipoCalculo.equals("MEDIA_PLANCHA")
                || nombre.contains("MEDIA PLANCHA")
                || nombre.equals("MEDIA")
                || nombre.contains("1/2 PLANCHA");
    }

    private int contarCortesQueCaben(
            List<Corte> cortes,
            BigDecimal largo,
            BigDecimal ancho
    ) {
        int cantidad = 0;

        for (Corte corte : cortes) {
            boolean normal =
                    corte.ancho.compareTo(largo) <= 0
                            && corte.alto.compareTo(ancho) <= 0;

            boolean rotado =
                    corte.alto.compareTo(largo) <= 0
                            && corte.ancho.compareTo(ancho) <= 0;

            if (normal || rotado) {
                cantidad++;
            }
        }

        return cantidad;
    }

    private void validarSolicitud(CalculoMedidasSolicitudDto s, Integer idSede) {
        if (s == null || s.idProducto() == null || s.idProducto() <= 0) throw new IllegalArgumentException("Debe seleccionar un producto.");
        if (idSede == null || idSede <= 0) throw new IllegalArgumentException("No existe una sede activa.");
        if (s.cortes() == null || s.cortes().isEmpty()) throw new IllegalArgumentException("Debe registrar medidas.");
        if (s.cortes().size() > 200) throw new IllegalArgumentException("Se excedió el máximo de grupos de medidas.");
        for (var c : s.cortes()) {
            if (c == null) {
                throw new IllegalArgumentException("Cada grupo de medidas debe contener datos válidos.");
            }
            if (c.cantidad() == null || c.cantidad() <= 0 || c.cantidad() > 10000) {
                throw new IllegalArgumentException("Cada grupo debe tener una cantidad entera válida.");
            }
        }
    }

    private void validarUnidadYTipo(CalculoMedidasSolicitudDto s, String tipo, String unidad) {
        if (s.tipoMedida() != null && !s.tipoMedida().isBlank() && !tipo.equals(normalizar(s.tipoMedida())))
            throw new IllegalArgumentException("El tipo de medida enviado no corresponde al producto.");
        if (s.unidadEntrada() != null && !s.unidadEntrada().isBlank() && !unidad.equals(normalizar(s.unidadEntrada())))
            throw new IllegalArgumentException("La unidad de entrada debe ser " + unidad + ".");
    }

    private BigDecimal convertirBase(BigDecimal valor, String tipo, String unidad) {
        if (!positivo(valor)) return valor;
        return tipo.equals("AREA") && unidad.equals("MM") ? valor.multiply(BigDecimal.valueOf(1000))
                : tipo.equals("LONGITUD") && unidad.equals("CM") ? valor.multiply(BigDecimal.valueOf(100)) : valor;
    }

    private String codigo(int[] counts, List<Fisica> fisicas) {
        StringBuilder sb = new StringBuilder("MIX");
        for (int i = 0; i < counts.length; i++) if (counts[i] > 0)
            sb.append('-').append(fisicas.get(i).presentacion.getIdPrecioPresentacion()).append('x').append(counts[i]);
        return sb.toString();
    }

    private boolean positivo(BigDecimal v) { return v != null && v.signum() > 0; }
    private String normalizar(String v) { return v == null ? "" : v.trim().toUpperCase(Locale.ROOT); }

    private record Corte(
            BigDecimal ancho,
            BigDecimal alto,
            BigDecimal longitud
    ) {
    }

    private record Fisica(
            ProductoPrecioPresentacion presentacion,
            BigDecimal largo,
            BigDecimal ancho,
            BigDecimal longitud,
            BigDecimal capacidadCobro,
            BigDecimal factorStock,
            Integer idParteInventario,
            String tipoParteInventario,
            String nombreInventario
    ) {
        private Fisica(ProductoPrecioPresentacion presentacion,
                       BigDecimal largo,
                       BigDecimal ancho,
                       BigDecimal longitud,
                       BigDecimal capacidadCobro,
                       BigDecimal factorStock) {
            this(presentacion, largo, ancho, longitud, capacidadCobro, factorStock,
                    null, null, null);
        }

        private Fisica conParteInventario(Integer idParte, String tipoParte, String nombre) {
            return new Fisica(presentacion, largo, ancho, longitud, capacidadCobro, factorStock,
                    idParte, tipoParte, nombre);
        }

        private Integer idPrecioPresentacion() {
            return presentacion == null ? null : presentacion.getIdPrecioPresentacion();
        }

        private String nombrePresentacion() {
            if (nombreInventario != null && !nombreInventario.isBlank()) return nombreInventario;
            return presentacion == null ? "Pieza física" : presentacion.getNombrePresentacion();
        }

        private boolean esRetazo() {
            return "RETAZO".equalsIgnoreCase(tipoParteInventario)
                    || "CORTE".equalsIgnoreCase(tipoParteInventario);
        }
    }

    private record Bin(
            BigDecimal largo,
            BigDecimal ancho,
            BigDecimal longitud
    ) {
    }

    private record ResidualArea(
            List<Integer> numerosCorte,
            BigDecimal areaM2
    ) {
    }

    /** Empaque tipo guillotina con rectángulos libres fusionables; permite rotación. */
    private static final class RectBin {
        private final List<Rect> libres = new ArrayList<>();
        RectBin(BigDecimal w, BigDecimal h) { libres.add(new Rect(BigDecimal.ZERO, BigDecimal.ZERO, w, h)); }
        boolean colocar(Corte corte) {
            OpcionRect mejor = null;
            for (int i = 0; i < libres.size(); i++) {
                Rect r = libres.get(i);
                mejor = mejorOpcion(mejor, opcion(i, r, corte.ancho, corte.alto, false));
                if (corte.ancho.compareTo(corte.alto) != 0) {
                    mejor = mejorOpcion(mejor, opcion(i, r, corte.alto, corte.ancho, true));
                }
            }
            if (mejor == null) return false;
            Rect r = libres.remove(mejor.indice);
            dividirLibre(libres, r, mejor.w, mejor.h);
            fusionarLibres(libres);
            return true;
        }
    }
    private record Rect(BigDecimal x, BigDecimal y, BigDecimal w, BigDecimal h) {}

    private record SuperficieComercial(
            BigDecimal ancho,
            BigDecimal alto,
            List<CortePlanoArea> cortes,
            List<Rect> libres
    ) {
        private BigDecimal desbalance() {
            return ancho.subtract(alto).abs();
        }
    }

    private static final class RectBinComercial {
        private final List<Rect> libres = new ArrayList<>();
        private final List<CortePlanoArea> cortes = new ArrayList<>();

        private RectBinComercial(BigDecimal ancho, BigDecimal alto) {
            libres.add(new Rect(BigDecimal.ZERO, BigDecimal.ZERO, ancho, alto));
        }

        private Ubicacion colocar(Corte corte) {
            OpcionRect mejor = null;
            for (int i = 0; i < libres.size(); i++) {
                Rect libre = libres.get(i);
                mejor = mejorOpcion(mejor, opcion(i, libre, corte.ancho, corte.alto, false));
                if (corte.ancho.compareTo(corte.alto) != 0) {
                    mejor = mejorOpcion(mejor, opcion(i, libre, corte.alto, corte.ancho, true));
                }
            }
            if (mejor == null) return null;
            Rect libre = libres.remove(mejor.indice);
            dividirLibre(libres, libre, mejor.w, mejor.h);
            fusionarLibres(libres);
            return new Ubicacion(libre.x, libre.y, mejor.w, mejor.h, mejor.rotado);
        }
    }

    private record CorteNumerado(
            int numero,
            Corte corte
    ) {
    }

    private record Ubicacion(
            BigDecimal x,
            BigDecimal y,
            BigDecimal w,
            BigDecimal h,
            boolean rotado
    ) {
    }

    private record CortePlanoArea(
            int numero,
            Corte original,
            Ubicacion ubicacion
    ) {
    }

    private record CortePlanoLongitud(
            int numero,
            BigDecimal inicio,
            BigDecimal longitud
    ) {
    }

    private static final class RectBinPlano {
        private final int numero;
        private final Fisica fisica;
        private final List<Rect> libres = new ArrayList<>();
        private final List<CortePlanoArea> cortes = new ArrayList<>();

        private RectBinPlano(int numero, Fisica fisica) {
            this.numero = numero;
            this.fisica = fisica;

            libres.add(
                    new Rect(
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            fisica.largo,
                            fisica.ancho
                    )
            );
        }

        private Ubicacion colocar(Corte corte) {
            OpcionRect mejor = null;
            for (int i = 0; i < libres.size(); i++) {
                Rect libre = libres.get(i);
                mejor = mejorOpcion(mejor, opcion(i, libre, corte.ancho, corte.alto, false));
                if (corte.ancho.compareTo(corte.alto) != 0) {
                    mejor = mejorOpcion(mejor, opcion(i, libre, corte.alto, corte.ancho, true));
                }
            }
            if (mejor == null) return null;
            Rect libre = libres.remove(mejor.indice);
            dividirLibre(libres, libre, mejor.w, mejor.h);
            fusionarLibres(libres);
            return new Ubicacion(libre.x, libre.y, mejor.w, mejor.h, mejor.rotado);
        }
    }

    private record OpcionRect(int indice, BigDecimal w, BigDecimal h, boolean rotado,
                              BigDecimal desperdicio, BigDecimal ladoCortoLibre) {}

    private static OpcionRect opcion(int indice, Rect libre, BigDecimal w, BigDecimal h, boolean rotado) {
        if (w.compareTo(libre.w) > 0 || h.compareTo(libre.h) > 0) return null;
        BigDecimal desperdicio = libre.w.multiply(libre.h).subtract(w.multiply(h));
        BigDecimal ladoCorto = libre.w.subtract(w).min(libre.h.subtract(h));
        return new OpcionRect(indice, w, h, rotado, desperdicio, ladoCorto);
    }

    private static OpcionRect mejorOpcion(OpcionRect actual, OpcionRect candidata) {
        if (candidata == null) return actual;
        if (actual == null) return candidata;
        int porArea = candidata.desperdicio.compareTo(actual.desperdicio);
        if (porArea != 0) return porArea < 0 ? candidata : actual;
        int porLado = candidata.ladoCortoLibre.compareTo(actual.ladoCortoLibre);
        if (porLado != 0) return porLado < 0 ? candidata : actual;
        if (candidata.rotado != actual.rotado) return candidata.rotado ? actual : candidata;
        return candidata.indice < actual.indice ? candidata : actual;
    }

    private static void dividirLibre(List<Rect> libres, Rect libre, BigDecimal w, BigDecimal h) {
        BigDecimal derecha = libre.w.subtract(w);
        BigDecimal abajo = libre.h.subtract(h);
        if (derecha.signum() > 0) {
            libres.add(new Rect(libre.x.add(w), libre.y, derecha, h));
        }
        if (abajo.signum() > 0) {
            libres.add(new Rect(libre.x, libre.y.add(h), libre.w, abajo));
        }
    }

    private static void fusionarLibres(List<Rect> libres) {
        boolean fusionado;
        do {
            fusionado = false;
            for (int i = 0; i < libres.size() && !fusionado; i++) {
                for (int j = i + 1; j < libres.size(); j++) {
                    Rect union = fusionRectangular(libres.get(i), libres.get(j));
                    if (union == null) continue;
                    libres.set(i, union);
                    libres.remove(j);
                    fusionado = true;
                    break;
                }
            }
        } while (fusionado);
        libres.sort(Comparator.comparing(Rect::y).thenComparing(Rect::x)
                .thenComparing(Rect::w).thenComparing(Rect::h));
    }

    private static Rect fusionRectangular(Rect a, Rect b) {
        if (a.x.compareTo(b.x) == 0 && a.w.compareTo(b.w) == 0) {
            if (a.y.add(a.h).compareTo(b.y) == 0) return new Rect(a.x, a.y, a.w, a.h.add(b.h));
            if (b.y.add(b.h).compareTo(a.y) == 0) return new Rect(a.x, b.y, a.w, a.h.add(b.h));
        }
        if (a.y.compareTo(b.y) == 0 && a.h.compareTo(b.h) == 0) {
            if (a.x.add(a.w).compareTo(b.x) == 0) return new Rect(a.x, a.y, a.w.add(b.w), a.h);
            if (b.x.add(b.w).compareTo(a.x) == 0) return new Rect(b.x, a.y, a.w.add(b.w), a.h);
        }
        return null;
    }

    private static final class BarraPlano {
        private final int numero;
        private final Fisica fisica;
        private BigDecimal restante;
        private final List<CortePlanoLongitud> cortes =
                new ArrayList<>();

        private BarraPlano(int numero, Fisica fisica) {
            this.numero = numero;
            this.fisica = fisica;
            this.restante = fisica.longitud;
        }
    }
    private String clasificarSobranteArea(BigDecimal areaM2) {
        if (areaM2 == null || areaM2.signum() <= 0) {
            return "MERMA_DESECHAR";
        }
        BigDecimal areaPie2 = areaM2.multiply(new BigDecimal("10.7639104167"));
        return areaPie2.compareTo(new BigDecimal("0.500000")) < 0
                ? "MERMA_DESECHAR"
                : "DentIA_INVENTARIO";
    }

}

