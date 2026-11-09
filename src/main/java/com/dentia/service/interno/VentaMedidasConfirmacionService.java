package com.dentia.service.interno;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dao.interfaces.IDetalleVentaDao;
import com.dentia.dto.interno.ventas.CalculoMedidasRespuestaDto;
import com.dentia.dto.interno.ventas.CalculoMedidasSolicitudDto;
import com.dentia.model.interno.DetalleVenta;
import com.dentia.model.interno.Producto;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import com.dentia.model.interno.Venta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recalcula y valida en el backend los grupos agregados mediante
 * "Agregar por medidas" antes de confirmar una venta.
 *
 * El navegador solo muestra una propuesta. Esta clase vuelve a ejecutar el
 * cálculo con datos actuales de producto, precios, presentaciones y stock.
 */
@Service
public class VentaMedidasConfirmacionService {

    private static final BigDecimal TOLERANCIA =
            new BigDecimal("0.000001");

    private final CalculoMedidasProvisionalService calculoService;
    private final ProductoService productoService;
    private final ProductoPrecioPresentacionService presentacionService;
    private final ObjectMapper objectMapper;
    private final IDetalleVentaDao detalleVentaDao;

    @Autowired
    public VentaMedidasConfirmacionService(
            CalculoMedidasProvisionalService calculoService,
            ProductoService productoService,
            ProductoPrecioPresentacionService presentacionService,
            ObjectMapper objectMapper,
            IDetalleVentaDao detalleVentaDao
    ) {
        this.calculoService = calculoService;
        this.productoService = productoService;
        this.presentacionService = presentacionService;
        this.objectMapper = objectMapper;
        this.detalleVentaDao = detalleVentaDao;
    }

    /*
     * Constructor conservado para pruebas antiguas que todavía no
     * proporcionen el DAO. En producción Spring usa el constructor
     * principal marcado con @Autowired.
     */
    VentaMedidasConfirmacionService(
            CalculoMedidasProvisionalService calculoService,
            ProductoService productoService,
            ObjectMapper objectMapper
    ) {
        this(
                calculoService,
                productoService,
                null,
                objectMapper,
                null
        );
    }

    public void validarYPreparar(Venta venta) {
        if (venta == null
                || venta.getDetalles() == null
                || venta.getDetalles().isEmpty()) {
            return;
        }

        Map<String, List<DetalleVenta>> grupos =
                new HashMap<>();

        for (DetalleVenta detalle : venta.getDetalles()) {
            String grupo =
                    normalizarGrupo(detalle.getGrupoMedidaUuid());

            if (grupo != null) {
                grupos.computeIfAbsent(
                        grupo,
                        ignored -> new ArrayList<>()
                ).add(detalle);
            }
        }

        for (Map.Entry<String, List<DetalleVenta>> entry
                : grupos.entrySet()) {
            validarGrupo(
                    venta,
                    entry.getKey(),
                    entry.getValue()
            );
        }

        /*
         * Las ventas normales no tienen grupoMedidaUuid. Cuando la
         * presentación representa área, longitud, media o pieza completa,
         * se crea aquí un plano automático y se enlaza con una parte real
         * del inventario antes de descontar stock.
         */
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (normalizarGrupo(detalle.getGrupoMedidaUuid()) == null) {
                prepararPlanoAutomaticoVentaNormal(venta, detalle);
            }
        }
    }

    private void prepararPlanoAutomaticoVentaNormal(
            Venta venta,
            DetalleVenta detalle
    ) {
        if (detalle == null
                || detalle.getIdPrecioPresentacion() == null
                || detalle.getIdPrecioPresentacion() <= 0
                || presentacionService == null) {
            return;
        }

        Producto producto = productoService.obtener(detalle.getIdProducto());
        if (producto == null || !Boolean.TRUE.equals(producto.getVentaPorMedida())) {
            return;
        }

        ProductoPrecioPresentacion presentacion = presentacionService
                .listarActivasPorProductoYSede(detalle.getIdProducto(), venta.getIdSede())
                .stream()
                .filter(p -> p != null
                        && detalle.getIdPrecioPresentacion().equals(p.getIdPrecioPresentacion()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "La presentación de la venta normal ya no está activa en la sede seleccionada."
                ));

        String tipoMedida = normalizar(producto.getTipoMedidaBase());
        String tipoCalculo = normalizar(presentacion.getTipoCalculoVenta());

        if (!("AREA".equals(tipoMedida) || "LONGITUD".equals(tipoMedida))
                || !requierePlanoAutomatico(tipoCalculo, presentacion.getNombrePresentacion())) {
            return;
        }

        int cantidad = obtenerCantidadEntera(detalle.getCantidad(), presentacion.getNombrePresentacion());
        String unidad = "AREA".equals(tipoMedida) ? "MM" : "CM";
        List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes = construirCortesAutomaticos(
                producto,
                presentacion,
                tipoMedida,
                tipoCalculo,
                cantidad
        );

        CalculoMedidasSolicitudDto solicitud = new CalculoMedidasSolicitudDto(
                detalle.getIdProducto(),
                tipoMedida,
                unidad,
                esPresentacionFisica(tipoCalculo, presentacion.getNombrePresentacion()),
                "Plano generado automáticamente desde una venta normal.",
                cortes,
                "COMPRAR_PRESENTACION",
                detalle.getIdPrecioPresentacion(),
                cantidad,
                null
        );

        CalculoMedidasRespuestaDto respuesta;
        try {
            respuesta = calculoService.calcular(solicitud, venta.getIdSede());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(
                    "No se pudo generar el plano automático para "
                            + presentacion.getNombrePresentacion() + ": " + ex.getMessage(),
                    ex
            );
        }

        validarPlanoConInventario(respuesta, "venta normal");

        List<CalculoMedidasRespuestaDto.LineaComercialDto> lineas =
                respuesta.combinacionRecomendada() == null
                        ? List.of()
                        : respuesta.combinacionRecomendada().lineas();

        CalculoMedidasRespuestaDto.LineaComercialDto linea = lineas == null
                ? null
                : lineas.stream()
                .filter(l -> detalle.getIdPrecioPresentacion().equals(l.idPrecioPresentacion()))
                .findFirst()
                .orElse(null);

        if (linea == null || !igualDecimal(detalle.getCantidad(), linea.cantidad())) {
            throw new RuntimeException(
                    "El plano automático cambió la cantidad comercial de la venta. "
                            + "Vuelva a agregar el producto."
            );
        }

        BigDecimal consumoStock = nvl(linea.cantidad())
                .multiply(nvlUno(linea.factorConversionStock()))
                .setScale(6, RoundingMode.HALF_UP);

        String planoJson = serializarPlanoCorte(respuesta.planoCorte(), "venta normal");
        String cortesJson = serializarSolicitudAutomatica(
                presentacion,
                detalle.getCantidad(),
                tipoMedida,
                unidad,
                cortes
        );

        detalle.setRequierePreparacion(true);
        detalle.setTipoPreparacion("CORTE");
        detalle.setEntregarSobrante(esPresentacionFisica(
                tipoCalculo,
                presentacion.getNombrePresentacion()
        ));
        detalle.setObservacionPreparacion("Plano generado automáticamente desde venta normal.");
        detalle.setCortesJson(cortesJson);
        detalle.setPlanoCorteJson(planoJson);
        detalle.setCantidadDescontadaStock(consumoStock);

        if (detalleVentaDao != null && detalle.getIdDetalleVenta() > 0) {
            int actualizados = detalleVentaDao.actualizarPreparacionAutomatica(
                    detalle.getIdDetalleVenta(),
                    true,
                    "CORTE",
                    detalle.isEntregarSobrante(),
                    detalle.getObservacionPreparacion(),
                    cortesJson,
                    planoJson,
                    consumoStock
            );
            if (actualizados != 1) {
                throw new RuntimeException(
                        "No se pudo guardar el plano automático del detalle "
                                + detalle.getIdDetalleVenta() + "."
                );
            }
        }
    }

    private boolean requierePlanoAutomatico(String tipoCalculo, String nombrePresentacion) {
        String nombre = normalizar(nombrePresentacion);
        return tipoCalculo.equals("PIE2")
                || tipoCalculo.equals("M2")
                || tipoCalculo.equals("METRO_CUADRADO")
                || tipoCalculo.equals("AREA")
                || tipoCalculo.equals("METRO_LINEAL")
                || tipoCalculo.equals("LONGITUD")
                || tipoCalculo.equals("MEDIA")
                || tipoCalculo.equals("COMPLETA")
                || nombre.contains("PLANCHA")
                || nombre.contains("VARILLA")
                || nombre.contains("ROLLO");
    }

    private boolean esPresentacionFisica(String tipoCalculo, String nombrePresentacion) {
        String nombre = normalizar(nombrePresentacion);
        return tipoCalculo.equals("MEDIA")
                || tipoCalculo.equals("COMPLETA")
                || nombre.contains("PLANCHA COMPLETA")
                || nombre.contains("MEDIA PLANCHA")
                || nombre.contains("1/2 PLANCHA")
                || nombre.contains("VARILLA COMPLETA")
                || nombre.contains("ROLLO COMPLETO");
    }

    private boolean esMediaFisica(String tipoCalculo, String nombrePresentacion) {
        String nombre = normalizar(nombrePresentacion);
        return tipoCalculo.equals("MEDIA")
                || nombre.contains("MEDIA PLANCHA")
                || nombre.contains("1/2 PLANCHA")
                || nombre.equals("MEDIA");
    }

    private int obtenerCantidadEntera(BigDecimal cantidad, String presentacion) {
        if (cantidad == null || cantidad.signum() <= 0
                || cantidad.stripTrailingZeros().scale() > 0) {
            throw new RuntimeException(
                    "La venta normal de " + presentacion
                            + " solo permite cantidades enteras mayores que cero."
            );
        }
        try {
            return cantidad.intValueExact();
        } catch (ArithmeticException ex) {
            throw new RuntimeException("La cantidad ingresada es demasiado grande.", ex);
        }
    }

    private List<CalculoMedidasSolicitudDto.CorteMedidaDto> construirCortesAutomaticos(
            Producto producto,
            ProductoPrecioPresentacion presentacion,
            String tipoMedida,
            String tipoCalculo,
            int cantidad
    ) {
        if ("AREA".equals(tipoMedida)) {
            BigDecimal anchoMm;
            BigDecimal altoMm;

            if (tipoCalculo.equals("PIE2")) {
                anchoMm = new BigDecimal("304.8").multiply(BigDecimal.valueOf(cantidad));
                altoMm = new BigDecimal("304.8");
            } else if (tipoCalculo.equals("M2")
                    || tipoCalculo.equals("METRO_CUADRADO")
                    || tipoCalculo.equals("AREA")) {
                anchoMm = new BigDecimal("1000").multiply(BigDecimal.valueOf(cantidad));
                altoMm = new BigDecimal("1000");
            } else {
                BigDecimal largoM = positivo(presentacion.getLargoBase())
                        ? presentacion.getLargoBase()
                        : producto.getLargoBase();
                BigDecimal anchoM = positivo(presentacion.getAnchoBase())
                        ? presentacion.getAnchoBase()
                        : producto.getAnchoBase();

                if (!positivo(largoM) || !positivo(anchoM)) {
                    throw new RuntimeException(
                            "La presentación física no tiene largo y ancho configurados."
                    );
                }

                if (esMediaFisica(tipoCalculo, presentacion.getNombrePresentacion())
                        && !positivo(presentacion.getLargoBase())
                        && !positivo(presentacion.getAnchoBase())) {
                    anchoM = anchoM.divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);
                }

                anchoMm = largoM.multiply(new BigDecimal("1000"));
                altoMm = anchoM.multiply(new BigDecimal("1000"));
            }

            return List.of(new CalculoMedidasSolicitudDto.CorteMedidaDto(
                    anchoMm,
                    altoMm,
                    null,
                    tipoCalculo.equals("PIE2")
                            || tipoCalculo.equals("M2")
                            || tipoCalculo.equals("METRO_CUADRADO")
                            || tipoCalculo.equals("AREA")
                            ? 1 : cantidad
            ));
        }

        BigDecimal longitudCm;
        if (tipoCalculo.equals("METRO_LINEAL") || tipoCalculo.equals("LONGITUD")) {
            longitudCm = new BigDecimal("100").multiply(BigDecimal.valueOf(cantidad));
            return List.of(new CalculoMedidasSolicitudDto.CorteMedidaDto(
                    null, null, longitudCm, 1
            ));
        }

        BigDecimal longitudM = positivo(presentacion.getMedidaTotalBase())
                ? presentacion.getMedidaTotalBase()
                : producto.getMedidaTotalBase();
        if (!positivo(longitudM)) {
            throw new RuntimeException(
                    "La presentación física no tiene una longitud configurada."
            );
        }
        if (esMediaFisica(tipoCalculo, presentacion.getNombrePresentacion())
                && !positivo(presentacion.getMedidaTotalBase())) {
            longitudM = longitudM.divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);
        }
        longitudCm = longitudM.multiply(new BigDecimal("100"));
        return List.of(new CalculoMedidasSolicitudDto.CorteMedidaDto(
                null, null, longitudCm, cantidad
        ));
    }

    private void validarPlanoConInventario(CalculoMedidasRespuestaDto respuesta, String contexto) {
        if (respuesta == null
                || respuesta.planoCorte() == null
                || respuesta.planoCorte().piezas() == null
                || respuesta.planoCorte().piezas().isEmpty()) {
            throw new RuntimeException("El cálculo de " + contexto + " no devolvió un plano físico válido.");
        }
        List<String> invalidos = respuesta.planoCorte().piezas().stream()
                .filter(java.util.Objects::nonNull)
                .filter(p -> p.idParteInventario() == null || p.idParteInventario() <= 0)
                .map(p -> p.codigo() == null ? "Plano " + p.numero() : p.codigo())
                .toList();
        if (!invalidos.isEmpty()) {
            throw new RuntimeException(
                    "El plano automático no pudo asignar una parte real de inventario a: "
                            + String.join(", ", invalidos) + "."
            );
        }
    }

    private String serializarSolicitudAutomatica(
            ProductoPrecioPresentacion presentacion,
            BigDecimal cantidad,
            String tipoMedida,
            String unidad,
            List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes
    ) {
        try {
            Map<String, Object> datos = new java.util.LinkedHashMap<>();
            datos.put("origen", "VENTA_NORMAL_AUTOMATICA");
            datos.put("modalidad", "COMPRAR_PRESENTACION");
            datos.put("idPrecioPresentacion", presentacion.getIdPrecioPresentacion());
            datos.put("cantidadPresentacion", cantidad.intValueExact());
            datos.put("tipoMedida", tipoMedida);
            datos.put("unidadEntrada", unidad);
            datos.put("cortesCalculoOriginal", cortes);
            return objectMapper.writeValueAsString(datos);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo guardar la solicitud automática de corte.", ex);
        }
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private void validarGrupo(
            Venta venta,
            String grupo,
            List<DetalleVenta> lineasGuardadas
    ) {
        DetalleVenta principal =
                lineasGuardadas.stream()
                        .filter(DetalleVenta::isLineaPrincipalMedida)
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "El grupo de medidas "
                                                + grupo
                                                + " no tiene una línea principal."
                                )
                        );

        Producto producto =
                productoService.obtener(
                        principal.getIdProducto()
                );

        if (producto == null
                || !Boolean.TRUE.equals(
                producto.getVentaPorMedida()
        )) {
            throw new RuntimeException(
                    "El producto del grupo "
                            + grupo
                            + " ya no está habilitado para venta por medidas."
            );
        }

        String tipo =
                normalizar(producto.getTipoMedidaBase());

        String unidad =
                switch (tipo) {
                    case "AREA" -> "MM";
                    case "LONGITUD" -> "CM";
                    default -> throw new RuntimeException(
                            "El producto del grupo "
                                    + grupo
                                    + " no tiene un tipo de medida válido."
                    );
                };

        DatosCalculoMedidas datos =
                leerDatosCalculo(
                        principal.getCortesJson(),
                        grupo
                );

        CalculoMedidasSolicitudDto solicitud =
                new CalculoMedidasSolicitudDto(
                        principal.getIdProducto(),
                        tipo,
                        unidad,
                        principal.isEntregarSobrante(),
                        principal.getObservacionPreparacion(),
                        datos.cortes(),
                        datos.modalidad(),
                        datos.idPrecioPresentacion(),
                        datos.cantidadPresentacion(),
                        datos.codigoCombinacion()
                );

        CalculoMedidasRespuestaDto respuesta;

        try {
            respuesta =
                    calculoService.calcular(
                            solicitud,
                            venta.getIdSede()
                    );
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(
                    "No se puede confirmar el grupo de medidas "
                            + grupo
                            + ": "
                            + ex.getMessage(),
                    ex
            );
        }

        if (respuesta == null
                || respuesta.combinacionRecomendada() == null) {
            throw new RuntimeException(
                    "No se obtuvo una combinación válida para el grupo "
                            + grupo
                            + "."
            );
        }

        if (respuesta.planoCorte() == null
                || respuesta.planoCorte().piezas() == null
                || respuesta.planoCorte().piezas().isEmpty()) {
            throw new RuntimeException(
                    "El cálculo del grupo "
                            + grupo
                            + " no devolvió un plano físico válido."
            );
        }

        List<CalculoMedidasRespuestaDto.PiezaFisicaDto> piezasSinInventario =
                respuesta.planoCorte()
                        .piezas()
                        .stream()
                        .filter(java.util.Objects::nonNull)
                        .filter(pieza ->
                                pieza.idParteInventario() == null
                                        || pieza.idParteInventario() <= 0
                        )
                        .toList();

        if (!piezasSinInventario.isEmpty()) {
            String planosInvalidos =
                    piezasSinInventario.stream()
                            .map(pieza ->
                                    pieza.codigo() != null
                                            ? pieza.codigo()
                                            : "Plano " + pieza.numero()
                            )
                            .collect(
                                    java.util.stream.Collectors.joining(", ")
                            );

            throw new RuntimeException(
                    "No se puede confirmar el grupo de medidas "
                            + grupo
                            + " porque los siguientes planos no tienen una pieza "
                            + "real de inventario asignada: "
                            + planosInvalidos
                            + ". Reemplace CalculoMedidasProvisionalService.java "
                            + "por la versión que envuelve el bloque comercial "
                            + "dentro de una pieza física."
            );
        }

        List<CalculoMedidasRespuestaDto.LineaComercialDto>
                esperadasCalculadas =
                respuesta.combinacionRecomendada().lineas();

        if (esperadasCalculadas == null
                || esperadasCalculadas.isEmpty()) {
            throw new RuntimeException(
                    "El cálculo del grupo "
                            + grupo
                            + " no devolvió líneas comerciales."
            );
        }

        List<CalculoMedidasRespuestaDto.LineaComercialDto> esperadas =
                seleccionarLineasActivas(
                        esperadasCalculadas,
                        datos.lineasActivas(),
                        grupo
                );

        if (lineasGuardadas.size() != esperadas.size()) {
            throw recalculoRequerido(grupo);
        }

        CalculoMedidasRespuestaDto.LineaComercialDto principalEsperada =
                esperadas.get(0);

        if (!igual(
                principal.getIdPrecioPresentacion(),
                principalEsperada.idPrecioPresentacion()
        ) || !igualDecimal(
                principal.getCantidad(),
                principalEsperada.cantidad()
        )) {
            throw recalculoRequerido(grupo);
        }

        List<FirmaLinea> guardadas =
                lineasGuardadas.stream()
                        .map(this::firmaGuardada)
                        .sorted(
                                Comparator
                                        .comparing(FirmaLinea::idPresentacion)
                                        .thenComparing(FirmaLinea::cantidad)
                        )
                        .toList();

        List<FirmaLinea> recalculadas =
                esperadas.stream()
                        .map(this::firmaEsperada)
                        .sorted(
                                Comparator
                                        .comparing(FirmaLinea::idPresentacion)
                                        .thenComparing(FirmaLinea::cantidad)
                        )
                        .toList();

        for (int i = 0; i < guardadas.size(); i++) {
            FirmaLinea guardada =
                    guardadas.get(i);

            FirmaLinea recalculada =
                    recalculadas.get(i);

            if (!guardada.coincideCon(recalculada)) {
                throw recalculoRequerido(grupo);
            }
        }

        BigDecimal consumoFisicoStock =
                esperadas.stream()
                        .map(linea ->
                                nvl(linea.cantidad())
                                        .multiply(
                                                nvlUno(
                                                        linea.factorConversionStock()
                                                )
                                        )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                6,
                                RoundingMode.HALF_UP
                        );

        if (consumoFisicoStock.compareTo(
                BigDecimal.ZERO
        ) <= 0) {
            throw new RuntimeException(
                    "El grupo de medidas "
                            + grupo
                            + " no tiene un consumo físico de inventario válido."
            );
        }

        String planoCorteJson =
                serializarPlanoCorte(
                        respuesta.planoCorte(),
                        grupo
                );

        /*
         * El plano validado se conserva únicamente en la línea principal.
         * cortesJson permanece intacto porque contiene la solicitud original.
         */
        principal.setPlanoCorteJson(planoCorteJson);
        principal.setCantidadDescontadaStock(
                consumoFisicoStock
        );

        for (DetalleVenta linea : lineasGuardadas) {
            if (!linea.isLineaPrincipalMedida()) {
                linea.setPlanoCorteJson(null);
                linea.setCantidadDescontadaStock(
                        BigDecimal.ZERO
                );
            }
        }

        /*
         * Durante la confirmación normal los detalles ya existen en la base.
         * Se persiste el plano recalculado antes de ejecutar el descuento.
         */
        if (detalleVentaDao != null
                && principal.getIdDetalleVenta() > 0) {
            int actualizados =
                    detalleVentaDao.actualizarPlanoCorteJson(
                            principal.getIdDetalleVenta(),
                            planoCorteJson
                    );

            if (actualizados != 1) {
                throw new RuntimeException(
                        "No se pudo guardar el plano validado del grupo "
                                + grupo
                                + "."
                );
            }
        }
    }

    private String serializarPlanoCorte(
            CalculoMedidasRespuestaDto.PlanoCorteDto plano,
            String grupo
    ) {
        try {
            return objectMapper.writeValueAsString(plano);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "No se pudo guardar el plano de corte del grupo "
                            + grupo
                            + ".",
                    ex
            );
        }
    }

    private DatosCalculoMedidas leerDatosCalculo(
            String cortesJson,
            String grupo
    ) {
        if (cortesJson == null
                || cortesJson.isBlank()) {
            throw new RuntimeException(
                    "El grupo de medidas "
                            + grupo
                            + " no conserva sus cortes."
            );
        }

        try {
            JsonNode raiz =
                    objectMapper.readTree(cortesJson);

            if (raiz.isArray()) {
                List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes =
                        objectMapper.convertValue(
                                raiz,
                                new TypeReference<
                                        List<
                                                CalculoMedidasSolicitudDto
                                                        .CorteMedidaDto
                                                >
                                        >() {
                                }
                        );

                return new DatosCalculoMedidas(
                        cortes,
                        "COTIZAR_CORTES",
                        null,
                        null,
                        null,
                        List.of()
                );
            }

            JsonNode nodoCortes =
                    raiz.path("cortesCalculoOriginal");

            if (!nodoCortes.isArray()
                    || nodoCortes.isEmpty()) {
                nodoCortes =
                        raiz.path("cortes");
            }

            if (!raiz.isObject()
                    || !nodoCortes.isArray()
                    || nodoCortes.isEmpty()) {
                throw new IllegalArgumentException(
                        "No contiene una lista de cortes."
                );
            }

            List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes =
                    objectMapper.convertValue(
                            nodoCortes,
                            new TypeReference<
                                    List<
                                            CalculoMedidasSolicitudDto
                                                    .CorteMedidaDto
                                            >
                                    >() {
                            }
                    );

            return new DatosCalculoMedidas(
                    cortes,
                    texto(
                            raiz,
                            "modalidad",
                            "COTIZAR_CORTES"
                    ),
                    enteroOpcional(
                            raiz,
                            "idPrecioPresentacion"
                    ),
                    enteroOpcional(
                            raiz,
                            "cantidadPresentacion"
                    ),
                    texto(
                            raiz,
                            "codigoCombinacion",
                            null
                    ),
                    leerLineasActivas(
                            raiz.path("uiGrupoMedidas")
                                    .path("lineas")
                    )
            );

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Los cortes del grupo "
                            + grupo
                            + " no tienen un formato válido.",
                    ex
            );
        }
    }

    private String texto(
            JsonNode nodo,
            String campo,
            String defecto
    ) {
        JsonNode valor =
                nodo.get(campo);

        return valor == null
                || valor.isNull()
                || valor.asText().isBlank()
                ? defecto
                : valor.asText().trim();
    }

    private Integer enteroOpcional(
            JsonNode nodo,
            String campo
    ) {
        JsonNode valor =
                nodo.get(campo);

        return valor != null
                && valor.canConvertToInt()
                ? valor.intValue()
                : null;
    }

    private List<FirmaLinea> leerLineasActivas(
            JsonNode nodoLineas
    ) {
        if (!nodoLineas.isArray()) {
            return List.of();
        }

        List<FirmaLinea> lineas =
                new ArrayList<>();

        for (JsonNode linea : nodoLineas) {
            if (linea.has("existente")
                    && !linea.path("existente")
                    .asBoolean(true)) {
                continue;
            }

            JsonNode id =
                    linea.get("idPrecioPresentacion");

            JsonNode cantidad =
                    linea.get("cantidad");

            JsonNode precio =
                    linea.get("precioUnitario");

            JsonNode factor =
                    linea.get("factorConversionStock");

            if (id == null
                    || !id.canConvertToInt()
                    || cantidad == null
                    || precio == null) {
                continue;
            }

            lineas.add(
                    new FirmaLinea(
                            id.intValue(),
                            escala(cantidad.decimalValue()),
                            escala(precio.decimalValue()),
                            escala(
                                    factor == null
                                            || factor.isNull()
                                            ? BigDecimal.ONE
                                            : factor.decimalValue()
                            ),
                            BigDecimal.ZERO.setScale(
                                    6,
                                    RoundingMode.HALF_UP
                            )
                    )
            );
        }

        return List.copyOf(lineas);
    }

    private List<CalculoMedidasRespuestaDto.LineaComercialDto>
    seleccionarLineasActivas(
            List<CalculoMedidasRespuestaDto.LineaComercialDto> calculadas,
            List<FirmaLinea> activas,
            String grupo
    ) {
        if (activas == null
                || activas.isEmpty()) {
            return calculadas;
        }

        List<CalculoMedidasRespuestaDto.LineaComercialDto> disponibles =
                new ArrayList<>(calculadas);

        List<CalculoMedidasRespuestaDto.LineaComercialDto> seleccionadas =
                new ArrayList<>();

        for (FirmaLinea activa : activas) {
            int indice = -1;

            for (int i = 0;
                 i < disponibles.size();
                 i++) {
                if (activa.coincideCon(
                        firmaEsperada(disponibles.get(i))
                )) {
                    indice = i;
                    break;
                }
            }

            if (indice < 0) {
                throw recalculoRequerido(grupo);
            }

            seleccionadas.add(
                    disponibles.remove(indice)
            );
        }

        return List.copyOf(seleccionadas);
    }

    private FirmaLinea firmaGuardada(
            DetalleVenta detalle
    ) {
        return new FirmaLinea(
                detalle.getIdPrecioPresentacion(),
                escala(detalle.getCantidad()),
                escala(detalle.getPrecioUnitario()),
                escala(
                        nvlUno(
                                detalle.getFactorConversionStock()
                        )
                ),
                BigDecimal.ZERO.setScale(
                        6,
                        RoundingMode.HALF_UP
                )
        );
    }

    private FirmaLinea firmaEsperada(
            CalculoMedidasRespuestaDto.LineaComercialDto linea
    ) {
        return new FirmaLinea(
                linea.idPrecioPresentacion(),
                escala(linea.cantidad()),
                escala(linea.precioUnitario()),
                escala(
                        nvlUno(
                                linea.factorConversionStock()
                        )
                ),
                BigDecimal.ZERO.setScale(
                        6,
                        RoundingMode.HALF_UP
                )
        );
    }

    private RuntimeException recalculoRequerido(
            String grupo
    ) {
        return new RuntimeException(
                "Las medidas, precios, presentaciones o el stock del grupo "
                        + grupo
                        + " cambiaron. Edite el grupo y pulse Calcular nuevamente antes de confirmar la venta."
        );
    }

    private boolean igual(
            Integer a,
            Integer b
    ) {
        return a != null
                && a.equals(b);
    }

    private boolean igualDecimal(
            BigDecimal a,
            BigDecimal b
    ) {
        return a != null
                && b != null
                && a.subtract(b)
                .abs()
                .compareTo(TOLERANCIA) <= 0;
    }

    private BigDecimal escala(
            BigDecimal valor
    ) {
        return nvl(valor).setScale(
                6,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal nvl(
            BigDecimal valor
    ) {
        return valor == null
                ? BigDecimal.ZERO
                : valor;
    }

    private BigDecimal nvlUno(
            BigDecimal valor
    ) {
        return valor == null
                ? BigDecimal.ONE
                : valor;
    }

    private String normalizarGrupo(
            String grupo
    ) {
        if (grupo == null) {
            return null;
        }

        String valor =
                grupo.trim();

        return valor.isEmpty()
                ? null
                : valor;
    }

    private String normalizar(
            String valor
    ) {
        return valor == null
                ? ""
                : valor.trim()
                .toUpperCase(Locale.ROOT);
    }

    private record FirmaLinea(
            Integer idPresentacion,
            BigDecimal cantidad,
            BigDecimal precio,
            BigDecimal factor,
            BigDecimal descuento
    ) {
        boolean coincideCon(
                FirmaLinea otra
        ) {
            return idPresentacion != null
                    && idPresentacion.equals(
                    otra.idPresentacion
            )
                    && cantidad.compareTo(
                    otra.cantidad
            ) == 0
                    && precio.compareTo(
                    otra.precio
            ) == 0
                    && factor.compareTo(
                    otra.factor
            ) == 0
                    && descuento.compareTo(
                    BigDecimal.ZERO
            ) == 0;
        }
    }

    private record DatosCalculoMedidas(
            List<CalculoMedidasSolicitudDto.CorteMedidaDto> cortes,
            String modalidad,
            Integer idPrecioPresentacion,
            Integer cantidadPresentacion,
            String codigoCombinacion,
            List<FirmaLinea> lineasActivas
    ) {
    }
}
