package com.dentia.service.interno;

import com.dentia.dao.interfaces.IProductoPrecioPresentacionDao;
import com.dentia.dao.interfaces.IProductoPresentacionOfertaDao;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import com.dentia.model.interno.ProductoPresentacionOferta;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ProductoPresentacionOfertaService {

    private final IProductoPresentacionOfertaDao ofertaDao;
    private final IProductoPrecioPresentacionDao precioDao;
    private final JdbcTemplate jdbc;

    public ProductoPresentacionOfertaService(IProductoPresentacionOfertaDao ofertaDao,
                                             IProductoPrecioPresentacionDao precioDao,
                                             JdbcTemplate jdbc) {
        this.ofertaDao = ofertaDao;
        this.precioDao = precioDao;
        this.jdbc = jdbc;
    }

    public Optional<ProductoPresentacionOferta> obtenerActiva(Integer idPrecioPresentacion) {
        validarIdPresentacion(idPrecioPresentacion);
        return ofertaDao.buscarActivaPorPresentacion(idPrecioPresentacion);
    }

    public ProductoPresentacionOferta guardarOferta(
            Integer idPrecioPresentacion,
            BigDecimal precioOferta,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String textoOferta,
            Boolean activo
    ) {
        ProductoPrecioPresentacion presentacion = obtenerPresentacionActiva(idPrecioPresentacion);
        ProductoPresentacionOferta oferta = construirOferta(
                idPrecioPresentacion,
                precioOferta,
                fechaInicio,
                fechaFin,
                textoOferta,
                activo
        );

        validarOferta(presentacion, oferta);

        Optional<ProductoPresentacionOferta> activaActual =
                ofertaDao.buscarActivaPorPresentacion(idPrecioPresentacion);

        if (activaActual.isPresent()) {
            oferta.setIdOferta(activaActual.get().getIdOferta());
            ofertaDao.actualizar(oferta);
        } else {
            ofertaDao.crear(oferta);
        }

        if (!Boolean.TRUE.equals(oferta.getActivo())) {
            return oferta;
        }

        return ofertaDao.buscarActivaPorPresentacion(idPrecioPresentacion)
                .orElse(oferta);
    }

    public void desactivarOferta(Integer idPrecioPresentacion) {
        obtenerPresentacionActiva(idPrecioPresentacion);
        ofertaDao.desactivarActivasPorPresentacion(idPrecioPresentacion);
    }

    private ProductoPresentacionOferta construirOferta(
            Integer idPrecioPresentacion,
            BigDecimal precioOferta,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String textoOferta,
            Boolean activo
    ) {
        ProductoPresentacionOferta oferta = new ProductoPresentacionOferta();
        oferta.setIdPrecioPresentacion(idPrecioPresentacion);
        oferta.setPrecioOferta(precioOferta == null ? null : precioOferta.setScale(2, RoundingMode.HALF_UP));
        oferta.setFechaInicio(fechaInicio);
        oferta.setFechaFin(fechaFin);
        oferta.setTextoOferta(normalizarTexto(textoOferta));
        oferta.setActivo(activo == null || activo);

        return oferta;
    }

    private ProductoPrecioPresentacion obtenerPresentacionActiva(Integer idPrecioPresentacion) {
        validarIdPresentacion(idPrecioPresentacion);

        ProductoPrecioPresentacion presentacion = precioDao.buscarPorId(idPrecioPresentacion)
                .orElseThrow(() -> new IllegalArgumentException("La presentacion seleccionada no existe."));

        if (presentacion.getEstado() == null || presentacion.getEstado() != 1) {
            throw new IllegalArgumentException("Solo se puede configurar oferta para una presentacion activa.");
        }

        return presentacion;
    }

    private void validarIdPresentacion(Integer idPrecioPresentacion) {
        if (idPrecioPresentacion == null || idPrecioPresentacion <= 0) {
            throw new IllegalArgumentException("Seleccione una presentación válida.");
        }
    }

    private void validarOferta(ProductoPrecioPresentacion presentacion, ProductoPresentacionOferta oferta) {
        if (oferta.getPrecioOferta() == null || oferta.getPrecioOferta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio de oferta debe ser mayor a cero.");
        }

        BigDecimal precioVenta = presentacion.getPrecioVenta();
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La presentación no tiene precio normal válido.");
        }

        if (oferta.getPrecioOferta().compareTo(precioVenta) >= 0) {
            throw new IllegalArgumentException("El precio de oferta debe ser menor al precio normal.");
        }

        if (oferta.getFechaInicio() == null) {
            throw new IllegalArgumentException("Ingrese la fecha de inicio de la oferta.");
        }

        if (oferta.getFechaFin() == null) {
            throw new IllegalArgumentException("Ingrese la fecha de fin de la oferta.");
        }

        LocalDate hoy = LocalDate.now();

        if (oferta.getFechaInicio().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a la fecha actual.");
        }

        if (oferta.getFechaFin().isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha actual.");
        }

        if (oferta.getFechaFin().isBefore(oferta.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio.");
        }
        BigDecimal costoPromedioActualSinIgv = obtenerCostoPromedioActual(presentacion.getIdProducto());

        BigDecimal factorConversion = presentacion.getFactorConversionStock();

        if (factorConversion == null || factorConversion.compareTo(BigDecimal.ZERO) <= 0) {
            factorConversion = BigDecimal.ONE;
        }

        BigDecimal costoBaseSinIgv = costoPromedioActualSinIgv
                .multiply(factorConversion)
                .setScale(2, RoundingMode.HALF_UP);

        if (costoBaseSinIgv.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal margenMinimo = BigDecimal.valueOf(10)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

            BigDecimal factorIgv = BigDecimal.valueOf(1.18);

            BigDecimal precioMinimoOferta = costoBaseSinIgv
                    .multiply(BigDecimal.ONE.add(margenMinimo))
                    .multiply(factorIgv)
                    .setScale(2, RoundingMode.HALF_UP);

            if (oferta.getPrecioOferta().compareTo(precioMinimoOferta) < 0) {
                throw new IllegalArgumentException(
                        "El precio de oferta no puede ser menor al precio mínimo permitido: S/ "
                                + precioMinimoOferta.setScale(2, RoundingMode.HALF_UP)
                );
            }
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        String limpio = texto.trim();
        return limpio.length() > 120 ? limpio.substring(0, 120) : limpio;
    }
    private BigDecimal obtenerCostoPromedioActual(Integer idProducto) {
        if (idProducto == null || idProducto <= 0) {
            return BigDecimal.ZERO;
        }

        String sql = """
        SELECT
            COALESCE(
                (
                    SELECT NULLIF(
                        SUM(st.costoPromedio * st.stockActual) / NULLIF(SUM(st.stockActual), 0),
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
            ) AS costoPromedio
        FROM productos p
        WHERE p.idProducto = ?
        LIMIT 1
    """;

        BigDecimal costo = jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return BigDecimal.ZERO;
            }

            BigDecimal valor = rs.getBigDecimal("costoPromedio");
            return valor != null ? valor : BigDecimal.ZERO;
        }, idProducto);

        return costo != null ? costo : BigDecimal.ZERO;
    }
}
