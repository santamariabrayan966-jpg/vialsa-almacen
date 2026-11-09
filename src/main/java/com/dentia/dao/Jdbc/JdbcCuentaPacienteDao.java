package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.model.paciente.CuentaPaciente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class JdbcCuentaPacienteDao implements ICuentaPacienteDao {

    private final JdbcTemplate jdbc;

    private final RowMapper<CuentaPaciente> mapper = (rs, rowNum) -> {
        CuentaPaciente cuenta = new CuentaPaciente();
        cuenta.setIdCuentaPaciente(rs.getLong("id_cuenta_paciente"));
        cuenta.setIdPaciente(rs.getLong("id_paciente"));
        cuenta.setCorreo(rs.getString("correo"));
        cuenta.setPasswordHash(rs.getString("password_hash"));
        cuenta.setActivo(rs.getBoolean("activo"));
        return cuenta;
    };

    public JdbcCuentaPacienteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void crear(CuentaPaciente cuenta) {
        jdbc.update("""
                INSERT INTO cuentas_paciente
                    (id_paciente, correo, password_hash, activo)
                VALUES (?, ?, ?, 1)
                """, cuenta.getIdPaciente(), normalizarCorreo(cuenta.getCorreo()), cuenta.getPasswordHash());
    }

    @Override
    public boolean existeCorreo(String correo) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cuentas_paciente WHERE correo = ?",
                Integer.class,
                normalizarCorreo(correo)
        );
        return total != null && total > 0;
    }

    @Override
    public Optional<CuentaPaciente> buscarPorCorreo(String correo) {
        return jdbc.query(
                "SELECT * FROM cuentas_paciente WHERE correo = ?",
                mapper,
                normalizarCorreo(correo)
        ).stream().findFirst();
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
