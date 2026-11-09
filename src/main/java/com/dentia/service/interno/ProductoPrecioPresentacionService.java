package com.dentia.service.interno;

import com.dentia.dao.interfaces.IProductoPrecioPresentacionDao;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductoPrecioPresentacionService {

    private final JdbcTemplate jdbc;
    private final IProductoPrecioPresentacionDao precioDao;

    public ProductoPrecioPresentacionService(JdbcTemplate jdbc,
                                             IProductoPrecioPresentacionDao precioDao) {
        this.jdbc = jdbc;
        this.precioDao = precioDao;
    }

    // ============================================================
    // LISTAR GENERAL POR PRODUCTO
    // Presentaciones y precios generales del producto.
    // ============================================================

    public List<ProductoPrecioPresentacion> listarPorProducto(Integer idProducto) {

        String sql = """
            SELECT
                ppp.idPrecioPresentacion,
                ppp.idProducto,
                ppp.idSede,
                ppp.nombrePresentacion,
                ppp.idUnidadVenta,
                ppp.tipoCalculoVenta,
                ppp.cantidadIncluida,
                ppp.largoBase,
                ppp.anchoBase,
                ppp.medidaTotalBase,
                ppp.factorConversionStock,
                ppp.precioVenta,
                ppp.descuentoMaximo,
                ppp.precioPortada,
                ofe.precioOferta AS precioOfertaPortada,
                CASE WHEN ofe.idOferta IS NULL THEN 0 ELSE 1 END AS tieneOfertaActiva,
                ofe.textoOferta AS textoOfertaPortada,
                ofe.fechaInicio AS fechaInicioOferta,
                ofe.fechaFin AS fechaFinOferta,
                ppp.precioBajoCosto,
                ppp.margenBajo,
                ppp.costoBaseCalculado,
                ppp.precioMinimoRecomendado,
                ppp.motivoPrecioBajoCosto,
                ppp.usuarioAutorizaPrecio,
                ppp.fechaAutorizacionPrecio,
                ppp.descripcionUso,
                ppp.estado,
                u.NombreUnidad AS nombreUnidadVenta,
                u.abreviatura AS abreviaturaUnidadVenta
            FROM producto_precio_presentacion ppp
            LEFT JOIN unidades u
                ON u.idUnidad = ppp.idUnidadVenta
            LEFT JOIN producto_presentacion_oferta ofe
                ON ofe.idOferta = (
                    SELECT o.idOferta
                    FROM producto_presentacion_oferta o
                    WHERE o.idPrecioPresentacion = ppp.idPrecioPresentacion
                      AND o.activo = 1
                      AND o.fechaInicio <= CURDATE()
                      AND o.fechaFin >= CURDATE()
                    ORDER BY o.idOferta DESC
                    LIMIT 1
                )
            WHERE ppp.idProducto = ?
            ORDER BY ppp.estado DESC, ppp.nombrePresentacion ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapearPrecio(rs), idProducto);
    }

    // ============================================================
    // LISTAR POR PRODUCTO + SEDE
    // Deprecated: las presentaciones son generales; la sede solo importa para stock/operaciones.
    // ============================================================

    public List<ProductoPrecioPresentacion> listarPorProductoYSede(Integer idProducto, Integer idSede) {

        if (idProducto == null || idProducto <= 0 || idSede == null || idSede <= 0) {
            return List.of();
        }

        return precioDao.listarPorProductoYSede(idProducto, idSede);
    }

    public List<ProductoPrecioPresentacion> listarActivasPorProducto(Integer idProducto) {

        if (idProducto == null || idProducto <= 0) {
            return List.of();
        }

        return listarPorProducto(idProducto)
                .stream()
                .filter(p -> p.getEstado() != null && p.getEstado() == 1)
                .toList();
    }

    public List<ProductoPrecioPresentacion> listarActivasPorProductoYSede(Integer idProducto, Integer idSede) {

        if (idProducto == null || idProducto <= 0 || idSede == null || idSede <= 0) {
            return List.of();
        }

        return listarPorProductoYSede(idProducto, idSede)
                .stream()
                .filter(p -> p.getEstado() != null && p.getEstado() == 1)
                .toList();
    }

    public ProductoPrecioPresentacion buscarActivaPorId(Integer idPrecioPresentacion) {
        if (idPrecioPresentacion == null || idPrecioPresentacion <= 0) {
            return null;
        }

        return precioDao.buscarPorId(idPrecioPresentacion)
                .filter(p -> p.getEstado() != null && p.getEstado() == 1)
                .orElse(null);
    }

    private ProductoPrecioPresentacion mapearPrecio(java.sql.ResultSet rs) throws java.sql.SQLException {

        ProductoPrecioPresentacion p = new ProductoPrecioPresentacion();

        p.setIdPrecioPresentacion(rs.getInt("idPrecioPresentacion"));
        p.setIdProducto(rs.getInt("idProducto"));
        p.setIdSede(rs.getObject("idSede") == null ? null : rs.getInt("idSede"));

        p.setNombrePresentacion(rs.getString("nombrePresentacion"));
        p.setIdUnidadVenta(rs.getInt("idUnidadVenta"));
        p.setTipoCalculoVenta(rs.getString("tipoCalculoVenta"));
        p.setCantidadIncluida(rs.getBigDecimal("cantidadIncluida"));
        p.setLargoBase(rs.getBigDecimal("largoBase"));
        p.setAnchoBase(rs.getBigDecimal("anchoBase"));
        p.setMedidaTotalBase(rs.getBigDecimal("medidaTotalBase"));
        p.setFactorConversionStock(rs.getBigDecimal("factorConversionStock"));
        p.setPrecioVenta(rs.getBigDecimal("precioVenta"));
        p.setDescuentoMaximo(rs.getBigDecimal("descuentoMaximo"));
        p.setPrecioPortada(rs.getObject("precioPortada") != null && rs.getBoolean("precioPortada"));
        p.setPrecioOfertaPortada(rs.getBigDecimal("precioOfertaPortada"));
        p.setTieneOfertaActiva(rs.getObject("tieneOfertaActiva") != null && rs.getBoolean("tieneOfertaActiva"));
        p.setTextoOfertaPortada(rs.getString("textoOfertaPortada"));
        p.setFechaInicioOferta(rs.getDate("fechaInicioOferta") == null ? null : rs.getDate("fechaInicioOferta").toLocalDate());
        p.setFechaFinOferta(rs.getDate("fechaFinOferta") == null ? null : rs.getDate("fechaFinOferta").toLocalDate());
        p.setPrecioBajoCosto(rs.getInt("precioBajoCosto") == 1);
        p.setMargenBajo(rs.getInt("margenBajo") == 1);
        p.setCostoBaseCalculado(rs.getBigDecimal("costoBaseCalculado"));
        p.setPrecioMinimoRecomendado(rs.getBigDecimal("precioMinimoRecomendado"));
        p.setMotivoPrecioBajoCosto(rs.getString("motivoPrecioBajoCosto"));
        p.setUsuarioAutorizaPrecio(rs.getString("usuarioAutorizaPrecio"));

        if (rs.getTimestamp("fechaAutorizacionPrecio") != null) {
            p.setFechaAutorizacionPrecio(
                    rs.getTimestamp("fechaAutorizacionPrecio").toLocalDateTime()
            );
        }

        p.setDescripcionUso(rs.getString("descripcionUso"));
        p.setEstado(rs.getInt("estado"));
        p.setNombreUnidadVenta(rs.getString("nombreUnidadVenta"));
        p.setAbreviaturaUnidadVenta(rs.getString("abreviaturaUnidadVenta"));

        return p;
    }

    // ============================================================
    // GUARDAR
    // ============================================================

    public void guardar(ProductoPrecioPresentacion form) {

        validar(form);
        validarDuplicadoPorProducto(form);

        if (form.getIdPrecioPresentacion() == null || form.getIdPrecioPresentacion() <= 0) {
            form.setIdPrecioPresentacion(insertar(form));
        } else {
            actualizar(form);
        }

    }

    private int insertar(ProductoPrecioPresentacion form) {

        String sql = """
            INSERT INTO producto_precio_presentacion
            (
                idProducto,
                idSede,
                nombrePresentacion,
                idUnidadVenta,
                tipoCalculoVenta,
                cantidadIncluida,
                largoBase,
                anchoBase,
                medidaTotalBase,
                factorConversionStock,
                precioVenta,
                descuentoMaximo,
                precioBajoCosto,
                margenBajo,
                costoBaseCalculado,
                precioMinimoRecomendado,
                motivoPrecioBajoCosto,
                usuarioAutorizaPrecio,
                fechaAutorizacionPrecio,
                descripcionUso,
                estado
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, form.getIdProducto());
            ps.setObject(2, null);
            ps.setString(3, form.getNombrePresentacion().trim());
            ps.setObject(4, form.getIdUnidadVenta());
            ps.setString(5, form.getTipoCalculoVenta());
            ps.setBigDecimal(6, form.getCantidadIncluida());
            ps.setBigDecimal(7, form.getLargoBase());
            ps.setBigDecimal(8, form.getAnchoBase());
            ps.setBigDecimal(9, form.getMedidaTotalBase());
            ps.setBigDecimal(10, form.getFactorConversionStock());
            ps.setBigDecimal(11, form.getPrecioVenta());
            ps.setBigDecimal(12, form.getDescuentoMaximo());
            ps.setInt(13, Boolean.TRUE.equals(form.getPrecioBajoCosto()) ? 1 : 0);
            ps.setInt(14, Boolean.TRUE.equals(form.getMargenBajo()) ? 1 : 0);
            ps.setBigDecimal(15, form.getCostoBaseCalculado());
            ps.setBigDecimal(16, form.getPrecioMinimoRecomendado());
            ps.setString(17, form.getMotivoPrecioBajoCosto());
            ps.setString(18, form.getUsuarioAutorizaPrecio());
            ps.setObject(19, form.getFechaAutorizacionPrecio());
            ps.setString(20, form.getDescripcionUso());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se pudo obtener el ID de la presentacion creada.");
        }

        return key.intValue();
    }

    private void actualizar(ProductoPrecioPresentacion form) {

        String sql = """
            UPDATE producto_precio_presentacion
            SET nombrePresentacion = ?,
                idUnidadVenta = ?,
                tipoCalculoVenta = ?,
                cantidadIncluida = ?,
                largoBase = ?,
                anchoBase = ?,
                medidaTotalBase = ?,
                factorConversionStock = ?,
                precioVenta = ?,
                descuentoMaximo = ?,
                precioBajoCosto = ?,
                margenBajo = ?,
                costoBaseCalculado = ?,
                precioMinimoRecomendado = ?,
                motivoPrecioBajoCosto = ?,
                usuarioAutorizaPrecio = ?,
                fechaAutorizacionPrecio = ?,
                descripcionUso = ?
            WHERE idPrecioPresentacion = ?
              AND idProducto = ?
        """;

        jdbc.update(
                sql,
                form.getNombrePresentacion().trim(),
                form.getIdUnidadVenta(),
                form.getTipoCalculoVenta(),
                form.getCantidadIncluida(),
                form.getLargoBase(),
                form.getAnchoBase(),
                form.getMedidaTotalBase(),
                form.getFactorConversionStock(),
                form.getPrecioVenta(),
                form.getDescuentoMaximo(),
                Boolean.TRUE.equals(form.getPrecioBajoCosto()) ? 1 : 0,
                Boolean.TRUE.equals(form.getMargenBajo()) ? 1 : 0,
                form.getCostoBaseCalculado(),
                form.getPrecioMinimoRecomendado(),
                form.getMotivoPrecioBajoCosto(),
                form.getUsuarioAutorizaPrecio(),
                form.getFechaAutorizacionPrecio(),
                form.getDescripcionUso(),
                form.getIdPrecioPresentacion(),
                form.getIdProducto()
        );
    }

    public void cambiarEstado(Integer idPrecioPresentacion, Integer estado) {

        if (idPrecioPresentacion == null || idPrecioPresentacion <= 0) {
            throw new IllegalArgumentException("Precio inválido.");
        }

        if (estado == null || (estado != 1 && estado != 2)) {
            throw new IllegalArgumentException("Estado inválido.");
        }

        String sql = """
            UPDATE producto_precio_presentacion
            SET estado = ?,
                precioPortada = CASE WHEN ? = 1 THEN precioPortada ELSE 0 END
            WHERE idPrecioPresentacion = ?
        """;

        jdbc.update(sql, estado, estado, idPrecioPresentacion);
    }

    public void marcarPrecioPortada(Integer idPrecioPresentacion) {
        if (idPrecioPresentacion == null || idPrecioPresentacion <= 0) {
            throw new IllegalArgumentException("Precio invalido.");
        }

        int filas = precioDao.marcarPrecioPortada(idPrecioPresentacion);

        if (filas == 0) {
            throw new IllegalArgumentException("No se pudo marcar la presentacion como precio portada.");
        }
    }

    // ============================================================
    // VALIDACIONES
    // ============================================================

    private void validar(ProductoPrecioPresentacion form) {

        if (form.getIdProducto() == null || form.getIdProducto() <= 0) {
            throw new IllegalArgumentException("Producto inválido.");
        }

        if (form.getNombrePresentacion() == null || form.getNombrePresentacion().trim().isBlank()) {
            throw new IllegalArgumentException("El nombre de la presentación es obligatorio.");
        }

        if (form.getIdUnidadVenta() == null || form.getIdUnidadVenta() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar la unidad de venta.");
        }

        if (form.getTipoCalculoVenta() == null || form.getTipoCalculoVenta().trim().isBlank()) {
            form.setTipoCalculoVenta("UNIDAD");
        }

        form.setTipoCalculoVenta(form.getTipoCalculoVenta().trim().toUpperCase());

// Compatibilidad con datos antiguos
        if ("CANTIDAD".equals(form.getTipoCalculoVenta())) {
            form.setTipoCalculoVenta("UNIDAD");
        } else if ("AREA".equals(form.getTipoCalculoVenta())) {
            form.setTipoCalculoVenta("PIE2");
        } else if ("LONGITUD".equals(form.getTipoCalculoVenta())) {
            form.setTipoCalculoVenta("METRO_LINEAL");
        }

        if (!form.getTipoCalculoVenta().equals("UNIDAD")
                && !form.getTipoCalculoVenta().equals("MEDIA")
                && !form.getTipoCalculoVenta().equals("PIE2")
                && !form.getTipoCalculoVenta().equals("METRO_LINEAL")) {
            throw new IllegalArgumentException("El tipo de venta no es válido.");
        }

        if (form.getCantidadIncluida() == null
                || form.getCantidadIncluida().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad incluida debe ser mayor a cero.");
        }

        if ((form.getTipoCalculoVenta().equals("UNIDAD")
                || form.getTipoCalculoVenta().equals("MEDIA")
                || form.getTipoCalculoVenta().equals("METRO_LINEAL"))
                && form.getCantidadIncluida().stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Para Unidad, Mitad o Metro lineal, la cantidad incluida debe ser un número entero.");
        }

        normalizarFactorConversionStock(form);

        if (form.getFactorConversionStock() == null
                || form.getFactorConversionStock().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El factor de conversión debe ser mayor a cero.");
        }

        if (form.getPrecioVenta() == null
                || form.getPrecioVenta().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de venta no puede ser negativo.");
        }

        if (form.getDescuentoMaximo() == null) {
            form.setDescuentoMaximo(BigDecimal.ZERO);
        }

        if (form.getDescuentoMaximo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento máximo no puede ser negativo.");
        }

        if (form.getDescuentoMaximo().compareTo(form.getPrecioVenta()) > 0) {
            throw new IllegalArgumentException("El descuento máximo no puede ser mayor al precio de venta.");
        }

        validarPrecioConCostoPromedio(form);
    }

    private void validarDuplicadoPorProducto(ProductoPrecioPresentacion form) {

        String sql = """
            SELECT COUNT(*)
            FROM producto_precio_presentacion
            WHERE idProducto = ?
              AND LOWER(TRIM(nombrePresentacion)) = LOWER(TRIM(?))
              AND (? IS NULL OR idPrecioPresentacion <> ?)
        """;

        Integer total = jdbc.queryForObject(
                sql,
                Integer.class,
                form.getIdProducto(),
                form.getNombrePresentacion(),
                form.getIdPrecioPresentacion(),
                form.getIdPrecioPresentacion()
        );

        if (total != null && total > 0) {
            throw new IllegalArgumentException("Ya existe una presentacion con ese nombre para el producto.");
        }
    }

    private void normalizarFactorConversionStock(ProductoPrecioPresentacion form) {

        String tipo = form.getTipoCalculoVenta();

        if ("UNIDAD".equals(tipo)) {
            form.setFactorConversionStock(
                    form.getCantidadIncluida().setScale(6, RoundingMode.HALF_UP)
            );
            return;
        }

        if ("MEDIA".equals(tipo)) {
            BigDecimal factor = form.getCantidadIncluida()
                    .multiply(new BigDecimal("0.5"))
                    .setScale(6, RoundingMode.HALF_UP);

            form.setFactorConversionStock(factor);
            return;
        }

        if ("PIE2".equals(tipo)) {

            Integer pie2VendibleBase = obtenerPie2VendibleBaseProducto(form.getIdProducto());

            if (pie2VendibleBase == null || pie2VendibleBase <= 0) {
                throw new IllegalArgumentException(
                        "El producto no tiene medida vendible configurada. "
                                + "Edita el producto y registra largo/ancho antes de crear precio por pie²."
                );
            }

            BigDecimal pie2PorPlancha = BigDecimal.valueOf(pie2VendibleBase);

            BigDecimal factor = form.getCantidadIncluida()
                    .divide(pie2PorPlancha, 6, RoundingMode.HALF_UP);

            form.setFactorConversionStock(factor);
            return;
        }

        if ("METRO_LINEAL".equals(tipo)) {

            BigDecimal metrosVendiblesBase = obtenerMetroLinealVendibleBaseProducto(form.getIdProducto());

            if (metrosVendiblesBase == null || metrosVendiblesBase.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "El producto no tiene metros vendibles configurados. " +
                                "Edita el producto y registra el largo real antes de crear precio por metro lineal."
                );
            }

            BigDecimal factor = form.getCantidadIncluida()
                    .divide(metrosVendiblesBase, 6, RoundingMode.HALF_UP);

            form.setFactorConversionStock(factor);
        }
    }

    private Integer obtenerPie2VendibleBaseProducto(Integer idProducto) {

        if (idProducto == null || idProducto <= 0) {
            return null;
        }

        String sql = """
        SELECT medidaVendibleBase
        FROM productos
        WHERE idProducto = ?
        LIMIT 1
    """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }

            Integer valor = rs.getObject("medidaVendibleBase") == null
                    ? null
                    : rs.getInt("medidaVendibleBase");

            return valor != null && valor > 0 ? valor : null;
        }, idProducto);
    }

    private BigDecimal obtenerMetroLinealVendibleBaseProducto(Integer idProducto) {

        if (idProducto == null || idProducto <= 0) {
            return null;
        }

        String sql = """
        SELECT medidaVendibleBase
        FROM productos
        WHERE idProducto = ?
          AND tipoMedidaBase = 'LONGITUD'
        LIMIT 1
    """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }

            BigDecimal valor = rs.getBigDecimal("medidaVendibleBase");

            if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }

            return valor;
        }, idProducto);
    }

    private void validarPrecioConCostoPromedio(ProductoPrecioPresentacion form) {

        CostoProducto costoProducto =
                obtenerCostoPromedioProducto(form.getIdProducto());

        if (costoProducto.costoPromedio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "No se puede guardar el precio de venta porque este producto aun no tiene costo promedio registrado. Primero registra una compra del producto y luego configura sus precios."
            );
        }

        recalcularCostoBaseYPrecioMinimo(form, costoProducto);

        BigDecimal costoPresentacionSinIgv = form.getCostoBaseCalculado();
        BigDecimal precioRecomendadoFinalConIgv = form.getPrecioMinimoRecomendado();
        BigDecimal factorIgv = obtenerFactorIgv();
        BigDecimal precioVentaFinalConIgv = form.getPrecioVenta()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioVentaBaseSinIgv = precioVentaFinalConIgv
                .divide(factorIgv, 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal descuentoMaximoFinalConIgv = form.getDescuentoMaximo() != null
                ? form.getDescuentoMaximo().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal gananciaNetaSinIgv = precioVentaBaseSinIgv
                .subtract(costoPresentacionSinIgv)
                .setScale(2, RoundingMode.HALF_UP);

        boolean esAdmin = usuarioActualEsAdmin();

        /*
         * REMATE:
         * El precio final incluye IGV.
         * Para saber si está bajo costo, se compara la BASE SIN IGV contra el costo SIN IGV.
         */
        if (precioVentaBaseSinIgv.compareTo(costoPresentacionSinIgv) < 0) {

            if (!esAdmin) {
                throw new IllegalArgumentException(
                        "El precio final con IGV deja una base sin IGV menor al costo real. Solo un ADMIN puede autorizar un remate."
                );
            }

            if (descuentoMaximoFinalConIgv.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException(
                        "En modo remate, el descuento máximo debe ser 0. El remate se aplica cambiando el precio final de venta."
                );
            }

            if (form.getMotivoPrecioBajoCosto() == null
                    || form.getMotivoPrecioBajoCosto().trim().isBlank()) {
                throw new IllegalArgumentException(
                        "Ingrese el motivo del remate para autorizar esta operación."
                );
            }

            form.setPrecioBajoCosto(true);
            form.setMargenBajo(true);
            form.setDescuentoMaximo(BigDecimal.ZERO);
            form.setMotivoPrecioBajoCosto(form.getMotivoPrecioBajoCosto().trim());
            form.setUsuarioAutorizaPrecio(obtenerUsuarioActual());
            form.setFechaAutorizacionPrecio(LocalDateTime.now());
            return;
        }

        /*
         * El descuento que ve el cliente está en precio final con IGV.
         * Por eso convertimos la ganancia neta disponible a monto final con IGV.
         */
        BigDecimal gananciaDisponibleFinalConIgv = gananciaNetaSinIgv
                .multiply(factorIgv)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal limiteDescuentoFinalConIgv = esAdmin
                ? gananciaDisponibleFinalConIgv
                : gananciaDisponibleFinalConIgv
                .multiply(BigDecimal.valueOf(0.50))
                .setScale(2, RoundingMode.HALF_UP);

        if (descuentoMaximoFinalConIgv.compareTo(limiteDescuentoFinalConIgv) > 0) {

            String mensaje = esAdmin
                    ? "El descuento máximo no puede superar toda la ganancia disponible. Máximo permitido: S/ "
                    : "El descuento máximo para trabajadores no puede superar el 50% de la ganancia. Máximo permitido: S/ ";

            throw new IllegalArgumentException(
                    mensaje + limiteDescuentoFinalConIgv.setScale(2, RoundingMode.HALF_UP)
            );
        }

        form.setPrecioBajoCosto(false);
        form.setMotivoPrecioBajoCosto(null);
        form.setUsuarioAutorizaPrecio(null);
        form.setFechaAutorizacionPrecio(null);

        form.setMargenBajo(precioVentaFinalConIgv.compareTo(precioRecomendadoFinalConIgv) < 0);
    }

    private BigDecimal obtenerFactorIgv() {
        BigDecimal tasaIgv = BigDecimal.valueOf(18)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        return BigDecimal.ONE.add(tasaIgv);
    }

    private void recalcularCostoBaseYPrecioMinimo(
            ProductoPrecioPresentacion form,
            CostoProducto costoProducto
    ) {
        BigDecimal factorCosto = calcularFactorCosto(form);

        BigDecimal costoBaseCalculado = costoProducto.costoPromedio()
                .multiply(factorCosto)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal margenMinimo = costoProducto.margenMinimoPorcentaje();

        if (margenMinimo == null || margenMinimo.compareTo(BigDecimal.ZERO) < 0) {
            margenMinimo = BigDecimal.TEN;
        }

        BigDecimal porcentajeMargen = margenMinimo
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        BigDecimal precioMinimoRecomendado = costoBaseCalculado
                .multiply(BigDecimal.ONE.add(porcentajeMargen))
                .multiply(obtenerFactorIgv())
                .setScale(2, RoundingMode.HALF_UP);

        form.setCostoBaseCalculado(costoBaseCalculado);
        form.setPrecioMinimoRecomendado(precioMinimoRecomendado);
    }

    private BigDecimal calcularFactorCosto(ProductoPrecioPresentacion form) {

        if (form.getFactorConversionStock() == null
                || form.getFactorConversionStock().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }

        return form.getFactorConversionStock();
    }

    private CostoProducto obtenerCostoPromedioProducto(Integer idProducto) {

        String sql = """
            SELECT
                COALESCE(
                    (
                        SELECT
                            NULLIF(
                                SUM(st.costoPromedio * st.stockActual)
                                    / NULLIF(SUM(st.stockActual), 0),
                                0
                            )
                        FROM inventario_stock st
                        WHERE st.idProducto = p.idProducto
                          AND st.activo = 1
                          AND st.stockActual > 0
                          AND COALESCE(st.costoPromedio, 0) > 0
                    ),
                    NULLIF(p.costoPromedio, 0),
                    0
                ) AS costoPromedio,
                COALESCE(NULLIF(p.margenMinimoPorcentaje, 0), 10) AS margenMinimoPorcentaje
            FROM productos p
            WHERE p.idProducto = ?
            LIMIT 1
        """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return new CostoProducto(BigDecimal.ZERO, BigDecimal.TEN);
            }

            BigDecimal costoPromedio = rs.getBigDecimal("costoPromedio");
            BigDecimal margenMinimo = rs.getBigDecimal("margenMinimoPorcentaje");

            if (costoPromedio == null) {
                costoPromedio = BigDecimal.ZERO;
            }

            if (margenMinimo == null) {
                margenMinimo = BigDecimal.TEN;
            }

            return new CostoProducto(costoPromedio, margenMinimo);
        }, idProducto);
    }

    private boolean usuarioActualEsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ADMIN".equals(a.getAuthority()));
    }

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null) {
            return "ADMIN";
        }

        return auth.getName();
    }

    private record CostoProducto(
            BigDecimal costoPromedio,
            BigDecimal margenMinimoPorcentaje
    ) {}
}
