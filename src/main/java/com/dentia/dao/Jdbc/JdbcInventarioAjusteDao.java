package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IInventarioAjusteDao;
import com.dentia.model.interno.inventario.InventarioAjuste;
import com.dentia.model.interno.inventario.InventarioAjusteDetalle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

@Repository
public class JdbcInventarioAjusteDao implements IInventarioAjusteDao {

    private final JdbcTemplate jdbc;

    public JdbcInventarioAjusteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int registrarAjuste(InventarioAjuste a) {
        String sql = """
            INSERT INTO inventario_ajuste
                                                (codigo, idSede, idUsuario, motivo, tipoAjuste, estado, observacion, evidenciaFoto)
                                                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, a.getCodigo());
            ps.setInt(2, a.getIdSede());

            if (a.getIdUsuario() != null) {
                ps.setInt(3, a.getIdUsuario());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, a.getMotivo());
            ps.setString(5, a.getTipoAjuste());
            ps.setString(6, a.getEstado());
            ps.setString(7, a.getObservacion());
            ps.setString(8, a.getEvidenciaFoto());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    @Override
    public void registrarDetalle(InventarioAjusteDetalle d) {
        String sql = """
            INSERT INTO inventario_ajuste_detalle
            (
                idAjuste,
                idProducto,
                tipoPresentacion,
                motivoDetalle,
                factorConversionStock,
                cantidad,
                cantidadDescontadaStock,
                stockAnterior,
                stockNuevo,
                valorPerdida,
                observacionDetalle
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbc.update(sql,
                d.getIdAjuste(),
                d.getIdProducto(),
                d.getTipoPresentacion(),
                d.getMotivoDetalle(),
                d.getFactorConversionStock(),
                d.getCantidad(),
                d.getCantidadDescontadaStock(),
                d.getStockAnterior(),
                d.getStockNuevo(),
                d.getValorPerdida(),
                d.getObservacionDetalle()
        );
    }

    @Override
    public List<InventarioAjuste> listarAjustes() {
        String sql = """
            SELECT 
                a.idAjuste,
                a.codigo,
                a.idSede,
                a.idUsuario,
                a.motivo,
                a.tipoAjuste,
                a.estado,
                a.observacion,
                a.fechaAjuste,
                s.nombre AS nombreSede,
                a.evidenciaFoto,
                u.nombreUsuario AS nombreUsuario
            FROM inventario_ajuste a
            INNER JOIN sedes s ON s.idSede = a.idSede
            LEFT JOIN usuarios u ON u.idUsuario = a.idUsuario
            ORDER BY a.fechaAjuste DESC
        """;

        List<InventarioAjuste> ajustes = jdbc.query(sql, (rs, rowNum) -> {
            InventarioAjuste a = new InventarioAjuste();

            a.setIdAjuste(rs.getInt("idAjuste"));
            a.setCodigo(rs.getString("codigo"));
            a.setIdSede(rs.getInt("idSede"));
            a.setIdUsuario(rs.getObject("idUsuario") == null ? null : rs.getInt("idUsuario"));
            a.setMotivo(rs.getString("motivo"));
            a.setTipoAjuste(rs.getString("tipoAjuste"));
            a.setEstado(rs.getString("estado"));
            a.setObservacion(rs.getString("observacion"));
            a.setNombreSede(rs.getString("nombreSede"));
            a.setEvidenciaFoto(rs.getString("evidenciaFoto"));
            a.setNombreUsuario(rs.getString("nombreUsuario"));

            if (rs.getTimestamp("fechaAjuste") != null) {
                a.setFechaAjuste(rs.getTimestamp("fechaAjuste").toLocalDateTime());
            }

            return a;
        });

        for (InventarioAjuste ajuste : ajustes) {
            ajuste.setDetalles(listarDetalles(ajuste.getIdAjuste()));
        }

        return ajustes;
    }

    @Override
    public List<InventarioAjuste> listarAjustesPorSede(Integer idSede) {
        String sql = """
            SELECT 
                a.idAjuste,
                a.codigo,
                a.idSede,
                a.idUsuario,
                a.motivo,
                a.tipoAjuste,
                a.estado,
                a.observacion,
                a.fechaAjuste,
                s.nombre AS nombreSede,
                a.evidenciaFoto,
                u.nombreUsuario AS nombreUsuario
            FROM inventario_ajuste a
            INNER JOIN sedes s ON s.idSede = a.idSede
            LEFT JOIN usuarios u ON u.idUsuario = a.idUsuario
            WHERE a.idSede = ?
            ORDER BY a.fechaAjuste DESC
        """;

        List<InventarioAjuste> ajustes = jdbc.query(sql, (rs, rowNum) -> {
            InventarioAjuste a = new InventarioAjuste();

            a.setIdAjuste(rs.getInt("idAjuste"));
            a.setCodigo(rs.getString("codigo"));
            a.setIdSede(rs.getInt("idSede"));
            a.setIdUsuario(rs.getObject("idUsuario") == null ? null : rs.getInt("idUsuario"));
            a.setMotivo(rs.getString("motivo"));
            a.setTipoAjuste(rs.getString("tipoAjuste"));
            a.setEstado(rs.getString("estado"));
            a.setObservacion(rs.getString("observacion"));
            a.setNombreSede(rs.getString("nombreSede"));
            a.setEvidenciaFoto(rs.getString("evidenciaFoto"));
            a.setNombreUsuario(rs.getString("nombreUsuario"));

            if (rs.getTimestamp("fechaAjuste") != null) {
                a.setFechaAjuste(rs.getTimestamp("fechaAjuste").toLocalDateTime());
            }

            return a;
        }, idSede);

        for (InventarioAjuste ajuste : ajustes) {
            ajuste.setDetalles(listarDetalles(ajuste.getIdAjuste()));
        }

        return ajustes;
    }

    @Override
    public Optional<InventarioAjuste> buscarPorId(Integer idAjuste) {
        String sql = """
        SELECT 
            a.idAjuste,
            a.codigo,
            a.idSede,
            a.idUsuario,
            a.motivo,
            a.tipoAjuste,
            a.estado,
            a.observacion,
            a.fechaAjuste,
            s.nombre AS nombreSede,
            u.nombreUsuario AS nombreUsuario
        FROM inventario_ajuste a
        INNER JOIN sedes s ON s.idSede = a.idSede
        LEFT JOIN usuarios u ON u.idUsuario = a.idUsuario
        WHERE a.idAjuste = ?
    """;

        List<InventarioAjuste> resultado = jdbc.query(sql, (rs, rowNum) -> {
            InventarioAjuste a = new InventarioAjuste();

            a.setIdAjuste(rs.getInt("idAjuste"));
            a.setCodigo(rs.getString("codigo"));
            a.setIdSede(rs.getInt("idSede"));
            a.setIdUsuario(rs.getObject("idUsuario") == null ? null : rs.getInt("idUsuario"));
            a.setMotivo(rs.getString("motivo"));
            a.setTipoAjuste(rs.getString("tipoAjuste"));
            a.setEstado(rs.getString("estado"));
            a.setObservacion(rs.getString("observacion"));
            a.setNombreSede(rs.getString("nombreSede"));
            a.setNombreUsuario(rs.getString("nombreUsuario"));

            if (rs.getTimestamp("fechaAjuste") != null) {
                a.setFechaAjuste(rs.getTimestamp("fechaAjuste").toLocalDateTime());
            }

            return a;
        }, idAjuste);

        return resultado.stream().findFirst();
    }

    @Override
    public List<InventarioAjusteDetalle> listarDetalles(Integer idAjuste) {
        String sql = """
            SELECT 
                d.idDetalleAjuste,
                d.idAjuste,
                d.idProducto,
                d.tipoPresentacion,
                d.motivoDetalle,
                d.factorConversionStock,
                d.cantidad,
                d.cantidadDescontadaStock,
                d.stockAnterior,
                d.stockNuevo,
                d.valorPerdida,
                d.observacionDetalle,
                p.CodigoInterno,
                p.NombreProducto
            FROM inventario_ajuste_detalle d
            INNER JOIN productos p ON p.idProducto = d.idProducto
            WHERE d.idAjuste = ?
            ORDER BY d.idDetalleAjuste ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            InventarioAjusteDetalle d = new InventarioAjusteDetalle();

            d.setIdDetalleAjuste(rs.getInt("idDetalleAjuste"));
            d.setIdAjuste(rs.getInt("idAjuste"));
            d.setIdProducto(rs.getInt("idProducto"));

            d.setTipoPresentacion(rs.getString("tipoPresentacion"));
            d.setMotivoDetalle(rs.getString("motivoDetalle"));
            d.setFactorConversionStock(rs.getBigDecimal("factorConversionStock"));

            d.setCantidad(rs.getBigDecimal("cantidad"));
            d.setCantidadDescontadaStock(rs.getBigDecimal("cantidadDescontadaStock"));
            d.setStockAnterior(rs.getBigDecimal("stockAnterior"));
            d.setStockNuevo(rs.getBigDecimal("stockNuevo"));
            d.setValorPerdida(rs.getBigDecimal("valorPerdida"));
            d.setObservacionDetalle(rs.getString("observacionDetalle"));

            d.setCodigoProducto(rs.getString("CodigoInterno"));
            d.setNombreProducto(rs.getString("NombreProducto"));

            return d;
        }, idAjuste);
    }

    @Override
    public String generarCodigo() {
        Integer siguiente = jdbc.queryForObject("""
            SELECT COALESCE(MAX(idAjuste), 0) + 1
            FROM inventario_ajuste
        """, Integer.class);

        return String.format("AJU-%05d", siguiente == null ? 1 : siguiente);
    }
}
