package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IInventarioMovimientoDao;
import com.dentia.model.interno.inventario.InventarioMovimiento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import java.util.List;

@Repository
public class JdbcInventarioMovimientoDao implements IInventarioMovimientoDao {

    private final JdbcTemplate jdbc;

    public JdbcInventarioMovimientoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<InventarioMovimiento> mapper = (rs, rowNum) -> {
        InventarioMovimiento m = new InventarioMovimiento();

        m.setIdMovimiento(rs.getInt("idMovimiento"));
        m.setIdProducto(rs.getInt("idProducto"));
        m.setIdSede(rs.getInt("idSede"));

        m.setCodigoProducto(rs.getString("CodigoInterno"));
        m.setNombreProducto(rs.getString("NombreProducto"));
        m.setNombreSede(rs.getString("nombreSede"));
        m.setTipoSede(rs.getString("tipoSede"));

        m.setTipoMovimiento(rs.getString("tipoMovimiento"));
        m.setOrigenMovimiento(rs.getString("origenMovimiento"));
        m.setDocumentoReferencia(rs.getString("documentoReferencia"));

        m.setEntrada(rs.getBigDecimal("entrada"));
        m.setSalida(rs.getBigDecimal("salida"));
        m.setStockResultante(rs.getBigDecimal("stockResultante"));
        m.setCostoUnitario(rs.getBigDecimal("costoUnitario"));

        m.setObservacion(rs.getString("observacion"));
        m.setIdUsuario(rs.getObject("idUsuario") == null ? null : rs.getInt("idUsuario"));
        m.setNombreUsuario(rs.getString("nombreUsuario"));

        if (rs.getTimestamp("fechaMovimiento") != null) {
            m.setFechaMovimiento(rs.getTimestamp("fechaMovimiento").toLocalDateTime());
        }

        return m;
    };

    @Override
    public List<InventarioMovimiento> listarMovimientos() {
        String sql = """
            SELECT
                mov.idMovimiento,
                mov.idProducto,
                mov.idSede,
                p.CodigoInterno,
                p.NombreProducto,
                se.nombre AS nombreSede,
                se.tipo AS tipoSede,
                mov.tipoMovimiento,
                mov.origenMovimiento,
                mov.documentoReferencia,
                mov.entrada,
                mov.salida,
                mov.stockResultante,
                mov.costoUnitario,
                mov.observacion,
                mov.idUsuario,
                u.nombreUsuario AS nombreUsuario,
                mov.fechaMovimiento
            FROM inventario_movimiento mov
            INNER JOIN productos p ON p.idProducto = mov.idProducto
            INNER JOIN sedes se ON se.idSede = mov.idSede
            LEFT JOIN usuarios u ON u.idUsuario = mov.idUsuario
            ORDER BY mov.fechaMovimiento DESC
        """;

        return jdbc.query(sql, mapper);
    }

    @Override
    public List<InventarioMovimiento> listarMovimientosPorSede(Integer idSede) {
        String sql = """
        SELECT
            mov.idMovimiento,
            mov.idProducto,
            mov.idSede,
            p.CodigoInterno,
            p.NombreProducto,
            se.nombre AS nombreSede,
            se.tipo AS tipoSede,
            mov.tipoMovimiento,
            mov.origenMovimiento,
            mov.documentoReferencia,
            mov.entrada,
            mov.salida,
            mov.stockResultante,
            mov.costoUnitario,
            mov.observacion,
            mov.idUsuario,
            u.nombreUsuario AS nombreUsuario,
            mov.fechaMovimiento
        FROM inventario_movimiento mov
        INNER JOIN productos p ON p.idProducto = mov.idProducto
        INNER JOIN sedes se ON se.idSede = mov.idSede
        LEFT JOIN usuarios u ON u.idUsuario = mov.idUsuario
        WHERE mov.idSede = ?
        ORDER BY mov.fechaMovimiento DESC
    """;

        return jdbc.query(sql, mapper, idSede);
    }

    @Override
    public int registrarMovimiento(InventarioMovimiento movimiento) {
        String sql = """
            INSERT INTO inventario_movimiento (
                idProducto,
                idSede,
                tipoMovimiento,
                origenMovimiento,
                documentoReferencia,
                entrada,
                salida,
                stockResultante,
                costoUnitario,
                observacion,
                idUsuario
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(sql,
                movimiento.getIdProducto(),
                movimiento.getIdSede(),
                movimiento.getTipoMovimiento(),
                movimiento.getOrigenMovimiento(),
                movimiento.getDocumentoReferencia(),
                movimiento.getEntrada(),
                movimiento.getSalida(),
                movimiento.getStockResultante(),
                movimiento.getCostoUnitario(),
                movimiento.getObservacion(),
                movimiento.getIdUsuario()
        );
    }

    @Override
    public int contarMovimientos() {
        String sql = "SELECT COUNT(*) FROM inventario_movimiento";
        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total != null ? total : 0;
    }

    @Override
    public int contarMovimientosPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(*)
        FROM inventario_movimiento
        WHERE idSede = ?
    """;

        Integer total = jdbc.queryForObject(sql, Integer.class, idSede);
        return total != null ? total : 0;
    }

    @Override
    public List<InventarioMovimiento> listarUltimosMovimientosPorProductoYSede(Integer idProducto, Integer idSede, int limite) {
        String sql = """
        SELECT
            mov.idMovimiento,
            mov.idProducto,
            mov.idSede,
            p.CodigoInterno,
            p.NombreProducto,
            se.nombre AS nombreSede,
            se.tipo AS tipoSede,
            mov.tipoMovimiento,
            mov.origenMovimiento,
            mov.documentoReferencia,
            mov.entrada,
            mov.salida,
            mov.stockResultante,
            mov.costoUnitario,
            mov.observacion,
            mov.idUsuario,
            u.nombreUsuario AS nombreUsuario,
            mov.fechaMovimiento
        FROM inventario_movimiento mov
        INNER JOIN productos p ON p.idProducto = mov.idProducto
        INNER JOIN sedes se ON se.idSede = mov.idSede
        LEFT JOIN usuarios u ON u.idUsuario = mov.idUsuario
        WHERE mov.idProducto = ?
          AND mov.idSede = ?
        ORDER BY mov.fechaMovimiento DESC
        LIMIT ?
    """;

        return jdbc.query(sql, mapper, idProducto, idSede, limite);
    }

    @Override
    public Optional<InventarioMovimiento> buscarPorId(Integer idMovimiento) {
        String sql = """
        SELECT
            mov.idMovimiento,
            mov.idProducto,
            mov.idSede,
            p.CodigoInterno,
            p.NombreProducto,
            se.nombre AS nombreSede,
            se.tipo AS tipoSede,
            mov.tipoMovimiento,
            mov.origenMovimiento,
            mov.documentoReferencia,
            mov.entrada,
            mov.salida,
            mov.stockResultante,
            mov.costoUnitario,
            mov.observacion,
            mov.idUsuario,
            u.nombreUsuario AS nombreUsuario,
            mov.fechaMovimiento
        FROM inventario_movimiento mov
        INNER JOIN productos p ON p.idProducto = mov.idProducto
        INNER JOIN sedes se ON se.idSede = mov.idSede
        LEFT JOIN usuarios u ON u.idUsuario = mov.idUsuario
        WHERE mov.idMovimiento = ?
    """;

        List<InventarioMovimiento> resultado = jdbc.query(sql, mapper, idMovimiento);

        return resultado.stream().findFirst();
    }

    @Override
    public List<InventarioMovimiento> listarMovimientosPorSedeConFiltros(
            Integer idSede,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        StringBuilder sql = new StringBuilder("""
        SELECT
            mov.idMovimiento,
            mov.idProducto,
            mov.idSede,
            p.CodigoInterno,
            p.NombreProducto,
            se.nombre AS nombreSede,
            se.tipo AS tipoSede,
            mov.tipoMovimiento,
            mov.origenMovimiento,
            mov.documentoReferencia,
            mov.entrada,
            mov.salida,
            mov.stockResultante,
            mov.costoUnitario,
            mov.observacion,
            mov.idUsuario,
            u.nombreUsuario AS nombreUsuario,
            mov.fechaMovimiento
        FROM inventario_movimiento mov
        INNER JOIN productos p ON p.idProducto = mov.idProducto
        INNER JOIN sedes se ON se.idSede = mov.idSede
        LEFT JOIN usuarios u ON u.idUsuario = mov.idUsuario
        WHERE mov.idSede = ?
    """);

        List<Object> params = new ArrayList<>();
        params.add(idSede);

        aplicarFiltros(sql, params, idProducto, busqueda, origenMovimiento, tipoMovimiento, fechaInicio, fechaFin);

        sql.append(" ORDER BY mov.fechaMovimiento DESC ");

        return jdbc.query(sql.toString(), mapper, params.toArray());
    }

    @Override
    public int contarMovimientosPorSedeConFiltros(
            Integer idSede,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*)
        FROM inventario_movimiento mov
        INNER JOIN productos p ON p.idProducto = mov.idProducto
        INNER JOIN sedes se ON se.idSede = mov.idSede
        LEFT JOIN usuarios u ON u.idUsuario = mov.idUsuario
        WHERE mov.idSede = ?
    """);

        List<Object> params = new ArrayList<>();
        params.add(idSede);

        aplicarFiltros(sql, params, idProducto, busqueda, origenMovimiento, tipoMovimiento, fechaInicio, fechaFin);

        Integer total = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return total != null ? total : 0;
    }

    private void aplicarFiltros(
            StringBuilder sql,
            List<Object> params,
            Integer idProducto,
            String busqueda,
            String origenMovimiento,
            String tipoMovimiento,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (idProducto != null && idProducto > 0) {
            sql.append(" AND mov.idProducto = ? ");
            params.add(idProducto);
        }

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append("""
            AND (
                LOWER(p.NombreProducto) LIKE ?
                OR LOWER(p.CodigoInterno) LIKE ?
                OR LOWER(mov.documentoReferencia) LIKE ?
                OR LOWER(mov.observacion) LIKE ?
                OR LOWER(mov.origenMovimiento) LIKE ?
            )
        """);

            String filtro = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(filtro);
            params.add(filtro);
            params.add(filtro);
            params.add(filtro);
            params.add(filtro);
        }

        if (origenMovimiento != null && !origenMovimiento.trim().isEmpty()) {
            sql.append(" AND mov.origenMovimiento = ? ");
            params.add(origenMovimiento.trim().toUpperCase());
        }

        if (tipoMovimiento != null && !tipoMovimiento.trim().isEmpty()) {
            sql.append(" AND mov.tipoMovimiento = ? ");
            params.add(tipoMovimiento.trim().toUpperCase());
        }

        if (fechaInicio != null) {
            sql.append(" AND mov.fechaMovimiento >= ? ");
            params.add(fechaInicio.atStartOfDay());
        }

        if (fechaFin != null) {
            sql.append(" AND mov.fechaMovimiento < ? ");
            params.add(fechaFin.plusDays(1).atStartOfDay());
        }
    }
}
