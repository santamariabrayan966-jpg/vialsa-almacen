package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IDetalleVentaDao;
import com.dentia.model.interno.DetalleVenta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcDetalleVentaDao implements IDetalleVentaDao {

    private final JdbcTemplate jdbc;

    public JdbcDetalleVentaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int registrar(DetalleVenta d) {
        String sql = """
            INSERT INTO detalleventa (
                idVentas,
                idProducto,
                idUnidad,
                idPrecioPresentacion,
                nombrePresentacion,
                factorConversionStock,
                cantidadDescontadaStock,
                Cantidad,
                PrecioUnitario,
                Descuento,
                requierePreparacion,
                tipoPreparacion,
                entregarSobrante,
                observacionPreparacion,
                cortesJson,
                planoCorteJson,
                croquisSvg,
                grupoMedidaUuid,
                lineaPrincipalMedida
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(
                sql,
                d.getIdVenta(),
                d.getIdProducto(),
                d.getIdUnidad(),
                d.getIdPrecioPresentacion(),
                d.getNombrePresentacion(),
                d.getFactorConversionStock(),
                d.getCantidadDescontadaStock(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getDescuento(),
                d.isRequierePreparacion(),
                d.getTipoPreparacion(),
                d.isEntregarSobrante(),
                d.getObservacionPreparacion(),
                d.getCortesJson(),
                d.getPlanoCorteJson(),
                d.getCroquisSvg(),
                d.getGrupoMedidaUuid(),
                d.isLineaPrincipalMedida()
        );
    }

    @Override
    public int registrarDetalleVentaOnline(Integer idVenta, DetalleVenta d) {
        String sql = """
            INSERT INTO detalleventa (
                idVentas,
                idProducto,
                idUnidad,
                idPrecioPresentacion,
                nombrePresentacion,
                factorConversionStock,
                cantidadDescontadaStock,
                Cantidad,
                PrecioUnitario,
                Descuento,
                precioNormal,
                precioOferta,
                precioFinalOnline,
                descuentoOferta,
                idOferta,
                requierePreparacion,
                tipoPreparacion,
                entregarSobrante,
                observacionPreparacion,
                cortesJson,
                planoCorteJson,
                croquisSvg,
                grupoMedidaUuid,
                lineaPrincipalMedida
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(
                sql,
                idVenta,
                d.getIdProducto(),
                d.getIdUnidad() > 0 ? d.getIdUnidad() : null,
                d.getIdPrecioPresentacion(),
                d.getNombrePresentacion(),
                d.getFactorConversionStock(),
                d.getCantidadDescontadaStock(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getDescuento(),
                d.getPrecioNormal(),
                d.getPrecioOferta(),
                d.getPrecioFinalOnline(),
                d.getDescuentoOferta(),
                d.getIdOferta(),
                d.isRequierePreparacion(),
                d.getTipoPreparacion(),
                d.isEntregarSobrante(),
                d.getObservacionPreparacion(),
                d.getCortesJson(),
                d.getPlanoCorteJson(),
                d.getCroquisSvg(),
                d.getGrupoMedidaUuid(),
                d.isLineaPrincipalMedida()
        );
    }

    @Override
    public List<DetalleVenta> listarPorVenta(int idVenta) {
        String sql = """
            SELECT
                dv.idDetalleVenta,
                dv.idVentas,
                dv.idProducto,
                dv.idUnidad,
                dv.idPrecioPresentacion,
                dv.nombrePresentacion,
                dv.factorConversionStock,
                dv.cantidadDescontadaStock,
                dv.Cantidad,
                dv.PrecioUnitario,
                dv.Descuento,
                dv.requierePreparacion,
                dv.tipoPreparacion,
                dv.entregarSobrante,
                dv.observacionPreparacion,
                CAST(dv.cortesJson AS CHAR) AS cortesJson,
                dv.planoCorteJson,
                dv.croquisSvg,
                dv.grupoMedidaUuid,
                dv.lineaPrincipalMedida,
                p.NombreProducto,
                p.foto,
                u.NombreUnidad
            FROM detalleventa dv
            INNER JOIN productos p
                ON dv.idProducto = p.idProducto
            LEFT JOIN unidades u
                ON dv.idUnidad = u.idUnidad
            WHERE dv.idVentas = ?
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            DetalleVenta d = new DetalleVenta();

            d.setIdDetalleVenta(rs.getInt("idDetalleVenta"));
            d.setIdVenta(rs.getInt("idVentas"));
            d.setIdProducto(rs.getInt("idProducto"));
            d.setIdUnidad(rs.getInt("idUnidad"));

            d.setIdPrecioPresentacion(
                    rs.getObject("idPrecioPresentacion") == null
                            ? null
                            : rs.getInt("idPrecioPresentacion")
            );

            d.setNombrePresentacion(rs.getString("nombrePresentacion"));
            d.setFactorConversionStock(rs.getBigDecimal("factorConversionStock"));
            d.setCantidadDescontadaStock(rs.getBigDecimal("cantidadDescontadaStock"));

            d.setCantidad(rs.getBigDecimal("Cantidad"));
            d.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
            d.setDescuento(rs.getBigDecimal("Descuento"));

            d.setRequierePreparacion(rs.getBoolean("requierePreparacion"));
            d.setTipoPreparacion(rs.getString("tipoPreparacion"));
            d.setEntregarSobrante(rs.getBoolean("entregarSobrante"));
            d.setObservacionPreparacion(rs.getString("observacionPreparacion"));
            d.setCortesJson(rs.getString("cortesJson"));
            d.setPlanoCorteJson(rs.getString("planoCorteJson"));
            d.setCroquisSvg(rs.getString("croquisSvg"));
            d.setGrupoMedidaUuid(rs.getString("grupoMedidaUuid"));
            d.setLineaPrincipalMedida(rs.getBoolean("lineaPrincipalMedida"));

            d.setNombreProducto(rs.getString("NombreProducto"));
            d.setFoto(rs.getString("foto"));
            d.setNombreUnidad(rs.getString("NombreUnidad"));

            return d;
        }, idVenta);
    }

    @Override
    public int actualizarPlanoCorteJson(
            Integer idDetalleVenta,
            String planoCorteJson
    ) {
        String sql = """
            UPDATE detalleventa
               SET planoCorteJson = ?
             WHERE idDetalleVenta = ?
        """;

        return jdbc.update(
                sql,
                planoCorteJson,
                idDetalleVenta
        );
    }

    @Override
    public int actualizarPreparacionAutomatica(
            Integer idDetalleVenta,
            boolean requierePreparacion,
            String tipoPreparacion,
            boolean entregarSobrante,
            String observacionPreparacion,
            String cortesJson,
            String planoCorteJson,
            java.math.BigDecimal cantidadDescontadaStock
    ) {
        String sql = """
            UPDATE detalleventa
               SET requierePreparacion = ?,
                   tipoPreparacion = ?,
                   entregarSobrante = ?,
                   observacionPreparacion = ?,
                   cortesJson = ?,
                   planoCorteJson = ?,
                   cantidadDescontadaStock = ?
             WHERE idDetalleVenta = ?
        """;

        return jdbc.update(
                sql,
                requierePreparacion,
                tipoPreparacion,
                entregarSobrante,
                observacionPreparacion,
                cortesJson,
                planoCorteJson,
                cantidadDescontadaStock,
                idDetalleVenta
        );
    }

    @Override
    public int eliminarPorVenta(int idVenta) {
        String sql = """
            DELETE FROM detalleventa
            WHERE idVentas = ?
        """;

        return jdbc.update(sql, idVenta);
    }
}