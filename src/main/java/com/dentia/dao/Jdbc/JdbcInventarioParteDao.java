package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IInventarioParteDao;
import com.dentia.model.interno.inventario.InventarioParte;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcInventarioParteDao implements IInventarioParteDao {

    private final JdbcTemplate jdbc;

    public JdbcInventarioParteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Integer getIntegerOrNull(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        Object value = rs.getObject(columnName);
        return value == null ? null : ((Number) value).intValue();
    }

    private final RowMapper<InventarioParte> mapper = (rs, rowNum) -> {
        InventarioParte p = new InventarioParte();

        p.setIdParte(rs.getInt("idParte"));
        p.setIdProducto(rs.getInt("idProducto"));
        p.setIdSede(rs.getInt("idSede"));

        p.setTipoParte(rs.getString("tipoParte"));
        p.setCantidadPartes(getIntegerOrNull(rs, "cantidadPartes"));
        p.setDescripcion(rs.getString("descripcion"));

        p.setTipoMedida(rs.getString("tipoMedida"));
        p.setUnidadMedida(rs.getString("unidadMedida"));

        p.setMedidaDisponible(getIntegerOrNull(rs, "medidaDisponible"));
        p.setMedidaPorParte(getIntegerOrNull(rs, "medidaPorParte"));
        p.setMedidaFisicaDisponible(rs.getBigDecimal("medidaFisicaDisponible"));

        p.setLargoDisponibleMm(rs.getBigDecimal("largoDisponibleMm"));
        p.setAnchoDisponibleMm(rs.getBigDecimal("anchoDisponibleMm"));
        p.setIdParteOrigen(getIntegerOrNull(rs, "idParteOrigen"));
        p.setIdDetalleVentaOrigen(getIntegerOrNull(rs, "idDetalleVentaOrigen"));
        p.setNumeroPlanoOrigen(getIntegerOrNull(rs, "numeroPlanoOrigen"));

        p.setEquivalenteUnidadFisica(rs.getBigDecimal("equivalenteUnidadFisica"));

        p.setCostoTotalOriginal(rs.getBigDecimal("costoTotalOriginal"));
        p.setCostoTotalDisponible(rs.getBigDecimal("costoTotalDisponible"));
        p.setCostoUnitarioParte(rs.getBigDecimal("costoUnitarioParte"));
        p.setCostoPorMedida(rs.getBigDecimal("costoPorMedida"));

        p.setEstado(rs.getString("estado"));

        Timestamp fechaRegistro = rs.getTimestamp("fechaRegistro");
        if (fechaRegistro != null) {
            p.setFechaRegistro(fechaRegistro.toLocalDateTime());
        }

        Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
        if (fechaActualizacion != null) {
            p.setFechaActualizacion(fechaActualizacion.toLocalDateTime());
        }

        return p;
    };

    @Override
    public int registrar(InventarioParte parte) {
        String sql = """
            INSERT INTO inventario_partes (
                idProducto,
                idSede,
                tipoParte,
                cantidadPartes,
                descripcion,
                tipoMedida,
                unidadMedida,
                medidaDisponible,
                medidaPorParte,
                medidaFisicaDisponible,
                largoDisponibleMm,
                anchoDisponibleMm,
                idParteOrigen,
                idDetalleVentaOrigen,
                numeroPlanoOrigen,
                equivalenteUnidadFisica,
                costoTotalOriginal,
                costoTotalDisponible,
                costoUnitarioParte,
                costoPorMedida,
                estado
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(sql,
                parte.getIdProducto(),
                parte.getIdSede(),
                parte.getTipoParte(),
                parte.getCantidadPartes() == null ? 1 : parte.getCantidadPartes(),
                parte.getDescripcion(),
                parte.getTipoMedida(),
                parte.getUnidadMedida(),
                parte.getMedidaDisponible(),
                parte.getMedidaPorParte(),
                parte.getMedidaFisicaDisponible(),
                parte.getLargoDisponibleMm(),
                parte.getAnchoDisponibleMm(),
                parte.getIdParteOrigen(),
                parte.getIdDetalleVentaOrigen(),
                parte.getNumeroPlanoOrigen(),
                parte.getEquivalenteUnidadFisica(),
                parte.getCostoTotalOriginal() == null ? BigDecimal.ZERO : parte.getCostoTotalOriginal(),
                parte.getCostoTotalDisponible() == null ? BigDecimal.ZERO : parte.getCostoTotalDisponible(),
                parte.getCostoUnitarioParte(),
                parte.getCostoPorMedida(),
                parte.getEstado() == null ? "DISPONIBLE" : parte.getEstado()
        );
    }

    @Override
    public List<InventarioParte> listarDisponiblesPorProductoYSede(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT *
            FROM inventario_partes
            WHERE idProducto = ?
              AND idSede = ?
              AND estado IN ('DISPONIBLE', 'PARCIAL')
            ORDER BY 
                CASE tipoParte
                    WHEN 'CORTE' THEN 1
                    WHEN 'RETAZO' THEN 1
                    WHEN 'MEDIA' THEN 2
                    WHEN 'COMPLETA' THEN 3
                    ELSE 4
                END,
                medidaDisponible ASC,
                idParte ASC
        """;

        return jdbc.query(sql, mapper, idProducto, idSede);
    }

    @Override
    public Optional<InventarioParte> buscarPorId(Integer idParte) {
        String sql = """
            SELECT *
            FROM inventario_partes
            WHERE idParte = ?
        """;

        List<InventarioParte> lista = jdbc.query(sql, mapper, idParte);
        return lista.stream().findFirst();
    }

    @Override
    public Optional<InventarioParte> buscarPorIdParaActualizar(Integer idParte) {
        String sql = """
            SELECT *
            FROM inventario_partes
            WHERE idParte = ?
            FOR UPDATE
        """;

        List<InventarioParte> lista = jdbc.query(sql, mapper, idParte);
        return lista.stream().findFirst();
    }

    @Override
    public Optional<InventarioParte> buscarLoteCompletoDisponible(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT *
            FROM inventario_partes
            WHERE idProducto = ?
              AND idSede = ?
              AND tipoParte = 'COMPLETA'
              AND estado IN ('DISPONIBLE', 'PARCIAL')
              AND cantidadPartes > 0
            ORDER BY idParte ASC
            LIMIT 1
        """;

        List<InventarioParte> lista = jdbc.query(sql, mapper, idProducto, idSede);
        return lista.stream().findFirst();
    }

    @Override
    public Optional<InventarioParte> buscarLoteCompletoPorProductoYSede(
            Integer idProducto, Integer idSede) {
        String sql = """
            SELECT *
              FROM inventario_partes
             WHERE idProducto = ?
               AND idSede = ?
               AND UPPER(tipoParte) = 'COMPLETA'
             ORDER BY idParte
             LIMIT 1
             FOR UPDATE
        """;
        List<InventarioParte> partes = jdbc.query(sql, mapper, idProducto, idSede);
        return partes.stream().findFirst();
    }

    @Override
    public List<InventarioParte> listarGeneradasPorDetalleVenta(Integer idDetalleVenta) {
        String sql = """
            SELECT *
              FROM inventario_partes
             WHERE idDetalleVentaOrigen = ?
             ORDER BY idParte
             FOR UPDATE
        """;
        return jdbc.query(sql, mapper, idDetalleVenta);
    }

    @Override
    public BigDecimal sumarEquivalenteDisponible(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT COALESCE(SUM(equivalenteUnidadFisica), 0)
            FROM inventario_partes
            WHERE idProducto = ?
              AND idSede = ?
              AND estado IN ('DISPONIBLE', 'PARCIAL')
        """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idProducto, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumarEquivalenteVendible(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT COALESCE(SUM(equivalenteUnidadFisica), 0)
            FROM inventario_partes
            WHERE idProducto = ?
              AND idSede = ?
              AND estado IN ('DISPONIBLE', 'PARCIAL')
        """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idProducto, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumarValorDisponible(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT COALESCE(SUM(costoTotalDisponible), 0)
            FROM inventario_partes
            WHERE idProducto = ?
              AND idSede = ?
              AND estado IN ('DISPONIBLE', 'PARCIAL')
        """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idProducto, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public void actualizarDisponibilidadParte(
            Integer idParte,
            Integer medidaDisponible,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            String estado
    ) {
        String sql = """
            UPDATE inventario_partes
               SET cantidadPartes = CASE WHEN ? = 'AGOTADA' THEN 0 ELSE cantidadPartes END,
                   medidaDisponible = ?,
                   equivalenteUnidadFisica = ?,
                   costoTotalDisponible = ?,
                   estado = ?,
                   fechaActualizacion = NOW()
             WHERE idParte = ?
        """;

        jdbc.update(sql,
                estado,
                medidaDisponible,
                equivalenteUnidadFisica,
                costoTotalDisponible,
                estado,
                idParte
        );
    }

    @Override
    public void restaurarParteIndividual(
            Integer idParte,
            Integer medidaDisponible,
            BigDecimal medidaFisicaDisponible,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            String estado
    ) {
        String sql = """
            UPDATE inventario_partes
               SET cantidadPartes = 1,
                   medidaDisponible = ?,
                   medidaFisicaDisponible = ?,
                   equivalenteUnidadFisica = ?,
                   costoTotalDisponible = ?,
                   estado = ?,
                   fechaActualizacion = NOW()
             WHERE idParte = ?
        """;

        jdbc.update(
                sql,
                medidaDisponible,
                medidaFisicaDisponible,
                equivalenteUnidadFisica,
                costoTotalDisponible,
                estado,
                idParte
        );
    }

    @Override
    public void actualizarLoteParte(
            Integer idParte,
            Integer cantidadPartes,
            Integer medidaDisponible,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            BigDecimal costoUnitarioParte,
            String estado
    ) {
        String sql = """
            UPDATE inventario_partes
               SET cantidadPartes = ?,
                   medidaDisponible = ?,
                   equivalenteUnidadFisica = ?,
                   costoTotalDisponible = ?,
                   costoUnitarioParte = ?,
                   estado = ?,
                   fechaActualizacion = NOW()
             WHERE idParte = ?
        """;

        jdbc.update(sql,
                cantidadPartes,
                medidaDisponible,
                equivalenteUnidadFisica,
                costoTotalDisponible,
                costoUnitarioParte,
                estado,
                idParte
        );
    }

    @Override
    public void incrementarLoteParte(
            Integer idParte,
            Integer cantidadAgregar,
            Integer medidaAgregar,
            BigDecimal equivalenteAgregar,
            BigDecimal costoAgregar,
            String estado
    ) {
        String sql = """
        UPDATE inventario_partes
           SET cantidadPartes = COALESCE(cantidadPartes, 0) + ?,
               medidaDisponible = COALESCE(medidaDisponible, 0) + ?,
               equivalenteUnidadFisica = COALESCE(equivalenteUnidadFisica, 0) + ?,
               costoTotalDisponible = COALESCE(costoTotalDisponible, 0) + ?,
               estado = ?,
               fechaActualizacion = NOW()
         WHERE idParte = ?
    """;

        jdbc.update(sql,
                cantidadAgregar,
                medidaAgregar,
                equivalenteAgregar,
                costoAgregar,
                estado,
                idParte
        );
    }

    @Override
    public void convertirParteEnCorte(
            Integer idParte,
            String tipoParte,
            Integer medidaDisponible,
            Integer medidaPorParte,
            BigDecimal largoDisponibleMm,
            BigDecimal anchoDisponibleMm,
            Integer idParteOrigen,
            Integer idDetalleVentaOrigen,
            Integer numeroPlanoOrigen,
            BigDecimal equivalenteUnidadFisica,
            BigDecimal costoTotalDisponible,
            BigDecimal costoUnitarioParte,
            BigDecimal costoPorMedida,
            String descripcion
    ) {
        String sql = """
            UPDATE inventario_partes
               SET tipoParte = ?,
                   cantidadPartes = 1,
                   descripcion = ?,
                   medidaDisponible = ?,
                   medidaPorParte = ?,
                   largoDisponibleMm = ?,
                   anchoDisponibleMm = ?,
                   idParteOrigen = ?,
                   idDetalleVentaOrigen = ?,
                   numeroPlanoOrigen = ?,
                   equivalenteUnidadFisica = ?,
                   costoTotalDisponible = ?,
                   costoUnitarioParte = ?,
                   costoPorMedida = ?,
                   estado = 'DISPONIBLE',
                   fechaActualizacion = NOW()
             WHERE idParte = ?
        """;

        jdbc.update(
                sql,
                tipoParte == null ? "CORTE" : tipoParte,
                descripcion,
                medidaDisponible,
                medidaPorParte,
                largoDisponibleMm,
                anchoDisponibleMm,
                idParteOrigen,
                idDetalleVentaOrigen,
                numeroPlanoOrigen,
                equivalenteUnidadFisica,
                costoTotalDisponible,
                costoUnitarioParte,
                costoPorMedida,
                idParte
        );
    }

    @Override
    public void actualizarGeometriaParte(
            Integer idParte,
            BigDecimal largoDisponibleMm,
            BigDecimal anchoDisponibleMm,
            Integer idParteOrigen,
            Integer idDetalleVentaOrigen,
            Integer numeroPlanoOrigen
    ) {
        String sql = """
            UPDATE inventario_partes
               SET largoDisponibleMm = ?,
                   anchoDisponibleMm = ?,
                   idParteOrigen = ?,
                   idDetalleVentaOrigen = ?,
                   numeroPlanoOrigen = ?,
                   fechaActualizacion = NOW()
             WHERE idParte = ?
        """;

        jdbc.update(
                sql,
                largoDisponibleMm,
                anchoDisponibleMm,
                idParteOrigen,
                idDetalleVentaOrigen,
                numeroPlanoOrigen,
                idParte
        );
    }
}