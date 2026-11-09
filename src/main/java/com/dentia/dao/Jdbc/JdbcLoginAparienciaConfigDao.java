package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ILoginAparienciaConfigDao;
import com.dentia.model.interno.LoginAparienciaConfig;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcLoginAparienciaConfigDao implements ILoginAparienciaConfigDao {

    private static final int ID_CONFIG = 1;

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginAparienciaConfigDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LoginAparienciaConfig obtenerConfiguracion() {
        String sql = """
            SELECT *
            FROM login_apariencia_config
            WHERE idConfig = ?
            LIMIT 1
        """;

        try {
            List<LoginAparienciaConfig> lista = jdbcTemplate.query(sql, this::mapear, ID_CONFIG);
            return lista.isEmpty() ? null : lista.get(0);
        } catch (BadSqlGrammarException e) {
            if (esTablaInexistente(e)) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public void guardar(LoginAparienciaConfig config) {
        if (existe()) {
            actualizar(config);
        } else {
            insertar(config);
        }
    }

    private boolean existe() {
        try {
            String sql = "SELECT COUNT(*) FROM login_apariencia_config WHERE idConfig = ?";
            Integer total = jdbcTemplate.queryForObject(sql, Integer.class, ID_CONFIG);
            return total != null && total > 0;
        } catch (BadSqlGrammarException e) {
            if (esTablaInexistente(e)) {
                return false;
            }
            throw e;
        }
    }

    private void insertar(LoginAparienciaConfig c) {
        String sql = """
            INSERT INTO login_apariencia_config (
                idConfig, tipoFondo, colorFondo1, colorFondo2, imagenFondo,
                posicionCard, titulo, subtitulo, textoInformativo, colorCard,
                colorTexto, colorBoton1, colorBoton2, colorTextoBoton,
                carruselActivo, carruselImagen1,
                carruselImagen1Zoom, carruselImagen1OffsetX, carruselImagen1OffsetY,
                carruselImagen2,
                carruselImagen2Zoom, carruselImagen2OffsetX, carruselImagen2OffsetY,
                carruselImagen3,
                carruselImagen3Zoom, carruselImagen3OffsetX, carruselImagen3OffsetY,
                fechaActualizacion
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        jdbcTemplate.update(sql,
                ID_CONFIG,
                c.getTipoFondo(),
                c.getColorFondo1(),
                c.getColorFondo2(),
                c.getImagenFondo(),
                c.getPosicionCard(),
                c.getTitulo(),
                c.getSubtitulo(),
                c.getTextoInformativo(),
                c.getColorCard(),
                c.getColorTexto(),
                c.getColorBoton1(),
                c.getColorBoton2(),
                c.getColorTextoBoton(),
                Boolean.TRUE.equals(c.getCarruselActivo()),
                c.getCarruselImagen1(),
                c.getCarruselImagen1Zoom(),
                c.getCarruselImagen1OffsetX(),
                c.getCarruselImagen1OffsetY(),
                c.getCarruselImagen2(),
                c.getCarruselImagen2Zoom(),
                c.getCarruselImagen2OffsetX(),
                c.getCarruselImagen2OffsetY(),
                c.getCarruselImagen3(),
                c.getCarruselImagen3Zoom(),
                c.getCarruselImagen3OffsetX(),
                c.getCarruselImagen3OffsetY());
    }

    private void actualizar(LoginAparienciaConfig c) {
        String sql = """
            UPDATE login_apariencia_config
               SET tipoFondo = ?,
                   colorFondo1 = ?,
                   colorFondo2 = ?,
                   imagenFondo = ?,
                   posicionCard = ?,
                   titulo = ?,
                   subtitulo = ?,
                   textoInformativo = ?,
                   colorCard = ?,
                   colorTexto = ?,
                   colorBoton1 = ?,
                   colorBoton2 = ?,
                   colorTextoBoton = ?,
                   carruselActivo = ?,
                   carruselImagen1 = ?,
                   carruselImagen1Zoom = ?,
                   carruselImagen1OffsetX = ?,
                   carruselImagen1OffsetY = ?,
                   carruselImagen2 = ?,
                   carruselImagen2Zoom = ?,
                   carruselImagen2OffsetX = ?,
                   carruselImagen2OffsetY = ?,
                   carruselImagen3 = ?,
                   carruselImagen3Zoom = ?,
                   carruselImagen3OffsetX = ?,
                   carruselImagen3OffsetY = ?,
                   fechaActualizacion = CURRENT_TIMESTAMP
             WHERE idConfig = ?
        """;

        jdbcTemplate.update(sql,
                c.getTipoFondo(),
                c.getColorFondo1(),
                c.getColorFondo2(),
                c.getImagenFondo(),
                c.getPosicionCard(),
                c.getTitulo(),
                c.getSubtitulo(),
                c.getTextoInformativo(),
                c.getColorCard(),
                c.getColorTexto(),
                c.getColorBoton1(),
                c.getColorBoton2(),
                c.getColorTextoBoton(),
                Boolean.TRUE.equals(c.getCarruselActivo()),
                c.getCarruselImagen1(),
                c.getCarruselImagen1Zoom(),
                c.getCarruselImagen1OffsetX(),
                c.getCarruselImagen1OffsetY(),
                c.getCarruselImagen2(),
                c.getCarruselImagen2Zoom(),
                c.getCarruselImagen2OffsetX(),
                c.getCarruselImagen2OffsetY(),
                c.getCarruselImagen3(),
                c.getCarruselImagen3Zoom(),
                c.getCarruselImagen3OffsetX(),
                c.getCarruselImagen3OffsetY(),
                ID_CONFIG);
    }

    private LoginAparienciaConfig mapear(ResultSet rs, int rowNum) throws SQLException {
        LoginAparienciaConfig c = new LoginAparienciaConfig();
        c.setIdConfig(rs.getInt("idConfig"));
        c.setTipoFondo(rs.getString("tipoFondo"));
        c.setColorFondo1(rs.getString("colorFondo1"));
        c.setColorFondo2(rs.getString("colorFondo2"));
        c.setImagenFondo(rs.getString("imagenFondo"));
        c.setPosicionCard(rs.getString("posicionCard"));
        c.setTitulo(rs.getString("titulo"));
        c.setSubtitulo(rs.getString("subtitulo"));
        c.setTextoInformativo(rs.getString("textoInformativo"));
        c.setColorCard(rs.getString("colorCard"));
        c.setColorTexto(rs.getString("colorTexto"));
        c.setColorBoton1(rs.getString("colorBoton1"));
        c.setColorBoton2(rs.getString("colorBoton2"));
        c.setColorTextoBoton(rs.getString("colorTextoBoton"));
        c.setCarruselActivo(existeColumna(rs, "carruselActivo") && rs.getBoolean("carruselActivo"));
        c.setCarruselImagen1(existeColumna(rs, "carruselImagen1") ? rs.getString("carruselImagen1") : null);
        c.setCarruselImagen1Zoom(decimal(rs, "carruselImagen1Zoom", "1.000"));
        c.setCarruselImagen1OffsetX(decimal(rs, "carruselImagen1OffsetX", "0.000"));
        c.setCarruselImagen1OffsetY(decimal(rs, "carruselImagen1OffsetY", "0.000"));
        c.setCarruselImagen2(existeColumna(rs, "carruselImagen2") ? rs.getString("carruselImagen2") : null);
        c.setCarruselImagen2Zoom(decimal(rs, "carruselImagen2Zoom", "1.000"));
        c.setCarruselImagen2OffsetX(decimal(rs, "carruselImagen2OffsetX", "0.000"));
        c.setCarruselImagen2OffsetY(decimal(rs, "carruselImagen2OffsetY", "0.000"));
        c.setCarruselImagen3(existeColumna(rs, "carruselImagen3") ? rs.getString("carruselImagen3") : null);
        c.setCarruselImagen3Zoom(decimal(rs, "carruselImagen3Zoom", "1.000"));
        c.setCarruselImagen3OffsetX(decimal(rs, "carruselImagen3OffsetX", "0.000"));
        c.setCarruselImagen3OffsetY(decimal(rs, "carruselImagen3OffsetY", "0.000"));

        Timestamp fecha = rs.getTimestamp("fechaActualizacion");
        c.setFechaActualizacion(fecha == null ? null : fecha.toLocalDateTime());
        return c;
    }

    private BigDecimal decimal(ResultSet rs, String columna, String defecto) throws SQLException {
        if (!existeColumna(rs, columna)) {
            return new BigDecimal(defecto);
        }
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? new BigDecimal(defecto) : valor;
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
}
