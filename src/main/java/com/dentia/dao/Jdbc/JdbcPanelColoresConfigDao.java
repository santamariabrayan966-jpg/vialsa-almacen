package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IPanelColoresConfigDao;
import com.dentia.model.interno.PanelColoresConfig;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcPanelColoresConfigDao implements IPanelColoresConfigDao {

    private static final int ID_CONFIG = 1;
    private static final String COLOR_FOOTER_DEFAULT = "#111827";
    private static final String COLOR_TEXTO_FOOTER_DEFAULT = "#ffffff";
    private static final String POSICION_MENU_DEFAULT = "ARRIBA";

    private final JdbcTemplate jdbcTemplate;

    public JdbcPanelColoresConfigDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PanelColoresConfig obtenerConfiguracion() {
        String sql = """
            SELECT *
            FROM panel_colores_config
            WHERE idConfig = ?
            LIMIT 1
        """;

        try {
            List<PanelColoresConfig> lista = jdbcTemplate.query(sql, this::mapear, ID_CONFIG);
            return lista.isEmpty() ? null : lista.get(0);
        } catch (BadSqlGrammarException e) {
            if (esTablaInexistente(e)) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public void guardar(PanelColoresConfig config) {
        try {
            if (existe()) {
                actualizar(config);
            } else {
                insertar(config);
            }
        } catch (BadSqlGrammarException e) {
            if (esColumnaPosicionMenuInexistente(e)) {
                throw new IllegalStateException("Falta ejecutar el SQL de posicionMenu en panel_colores_config.", e);
            }
            throw e;
        }
    }

    private boolean existe() {
        String sql = "SELECT COUNT(*) FROM panel_colores_config WHERE idConfig = ?";
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, ID_CONFIG);
        return total != null && total > 0;
    }

    private void insertar(PanelColoresConfig c) {
        String sql = """
            INSERT INTO panel_colores_config (
                idConfig, colorHeader, colorHeaderSecundario, colorFondo, colorTarjeta,
                colorBotonPrimario, colorAcento, colorTextoPrincipal, colorTextoHeader,
                colorFooter, colorTextoFooter, posicionMenu,
                fechaActualizacion
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        jdbcTemplate.update(sql,
                ID_CONFIG,
                c.getColorHeader(),
                c.getColorHeaderSecundario(),
                c.getColorFondo(),
                c.getColorTarjeta(),
                c.getColorBotonPrimario(),
                c.getColorAcento(),
                c.getColorTextoPrincipal(),
                c.getColorTextoHeader(),
                c.getColorFooter(),
                c.getColorTextoFooter(),
                c.getPosicionMenu());
    }

    private void actualizar(PanelColoresConfig c) {
        String sql = """
            UPDATE panel_colores_config
               SET colorHeader = ?,
                   colorHeaderSecundario = ?,
                   colorFondo = ?,
                   colorTarjeta = ?,
                   colorBotonPrimario = ?,
                   colorAcento = ?,
                   colorTextoPrincipal = ?,
                   colorTextoHeader = ?,
                   colorFooter = ?,
                   colorTextoFooter = ?,
                   posicionMenu = ?,
                   fechaActualizacion = CURRENT_TIMESTAMP
             WHERE idConfig = ?
        """;

        jdbcTemplate.update(sql,
                c.getColorHeader(),
                c.getColorHeaderSecundario(),
                c.getColorFondo(),
                c.getColorTarjeta(),
                c.getColorBotonPrimario(),
                c.getColorAcento(),
                c.getColorTextoPrincipal(),
                c.getColorTextoHeader(),
                c.getColorFooter(),
                c.getColorTextoFooter(),
                c.getPosicionMenu(),
                ID_CONFIG);
    }

    private PanelColoresConfig mapear(ResultSet rs, int rowNum) throws SQLException {
        PanelColoresConfig c = new PanelColoresConfig();
        c.setIdConfig(rs.getInt("idConfig"));
        c.setColorHeader(rs.getString("colorHeader"));
        c.setColorHeaderSecundario(rs.getString("colorHeaderSecundario"));
        c.setColorFondo(rs.getString("colorFondo"));
        c.setColorTarjeta(rs.getString("colorTarjeta"));
        c.setColorBotonPrimario(rs.getString("colorBotonPrimario"));
        c.setColorAcento(rs.getString("colorAcento"));
        c.setColorTextoPrincipal(rs.getString("colorTextoPrincipal"));
        c.setColorTextoHeader(rs.getString("colorTextoHeader"));
        // Compatibilidad temporal: si falta ejecutar el ALTER TABLE, se leen los colores antiguos
        // y se usan defaults para footer. Guardar footer requiere las columnas nuevas.
        c.setColorFooter(existeColumna(rs, "colorFooter")
                ? rs.getString("colorFooter")
                : COLOR_FOOTER_DEFAULT);
        c.setColorTextoFooter(existeColumna(rs, "colorTextoFooter")
                ? rs.getString("colorTextoFooter")
                : COLOR_TEXTO_FOOTER_DEFAULT);
        c.setPosicionMenu(existeColumna(rs, "posicionMenu")
                ? rs.getString("posicionMenu")
                : POSICION_MENU_DEFAULT);

        Timestamp fecha = rs.getTimestamp("fechaActualizacion");
        c.setFechaActualizacion(fecha == null ? null : fecha.toLocalDateTime());
        return c;
    }

    private boolean existeColumna(ResultSet rs, String columna) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columna.equalsIgnoreCase(metaData.getColumnLabel(i))
                    || columna.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean esTablaInexistente(BadSqlGrammarException e) {
        SQLException sqlException = e.getSQLException();
        String estado = sqlException != null ? sqlException.getSQLState() : null;
        String mensaje = sqlException != null ? sqlException.getMessage() : "";
        return "42S02".equals(estado)
                || (mensaje != null && mensaje.toLowerCase().contains("doesn't exist"));
    }

    private boolean esColumnaPosicionMenuInexistente(BadSqlGrammarException e) {
        SQLException sqlException = e.getSQLException();
        String mensaje = sqlException != null ? sqlException.getMessage() : "";
        return mensaje != null
                && mensaje.toLowerCase().contains("posicionmenu")
                && mensaje.toLowerCase().contains("unknown column");
    }
}
