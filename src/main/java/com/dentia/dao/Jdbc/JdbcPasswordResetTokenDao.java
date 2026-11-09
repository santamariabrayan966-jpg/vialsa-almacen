package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.PasswordResetTokenDao;
import com.dentia.model.auth.PasswordResetToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

@Repository
public class JdbcPasswordResetTokenDao implements PasswordResetTokenDao {

    private final JdbcTemplate jdbc;

    public JdbcPasswordResetTokenDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardarToken(PasswordResetToken token) {

        String sql = """
    INSERT INTO password_reset_tokens
                (id_usuario, codigo, fecha_creacion, fecha_expiracion, usado)
                VALUES (?, ?, ?, ?, ?)
    """;

        jdbc.update(
                sql,
                token.getIdUsuario(),
                token.getCodigo(),
                token.getFechaCreacion(),
                token.getFechaExpiracion(),
                token.getUsado()
        );
    }

    @Override
    public void invalidarTokensUsuario(Integer idUsuario) {
        jdbc.update("UPDATE password_reset_tokens SET usado = 1 WHERE id_usuario = ? AND usado = 0", idUsuario);
    }

    @Override
    public boolean consumirToken(Integer idToken) {

        String sql = """
            UPDATE password_reset_tokens
            SET usado = 1
            WHERE id_token = ? AND usado = 0 AND fecha_expiracion > NOW()
            """;

        return jdbc.update(sql, idToken) == 1;
    }

    @Override
    public Optional<PasswordResetToken> buscarPorCodigo(String codigo) {

        String sql = """
        SELECT *
        FROM password_reset_tokens
                WHERE codigo = ?
                AND usado = 0
                ORDER BY fecha_creacion DESC
                LIMIT 1
        """;

        return jdbc.query(sql, (ResultSet rs) -> {

            if (!rs.next()) {
                return Optional.empty();
            }

            PasswordResetToken t = new PasswordResetToken();

            t.setIdToken(rs.getInt("id_token"));
            t.setIdUsuario(rs.getInt("id_usuario"));
            t.setCodigo(rs.getString("codigo"));

            t.setFechaCreacion(
                    rs.getTimestamp("fecha_creacion").toLocalDateTime()
            );

            t.setFechaExpiracion(
                    rs.getTimestamp("fecha_expiracion").toLocalDateTime()
            );

            t.setUsado(rs.getBoolean("usado"));

            return Optional.of(t);

        }, codigo);
    }
}
