package com.dentia.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@org.junit.jupiter.api.Tag("integration")
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RolSchemaIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void esquemaRealDeRolesCoincideConContratoDentia() {
        List<Columna> columnas = jdbc.query("""
                SELECT column_name, column_type, is_nullable, column_default, extra
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'roles'
                ORDER BY ordinal_position
                """, (rs, row) -> new Columna(rs.getString("column_name"), rs.getString("column_type"),
                rs.getString("is_nullable"), rs.getString("column_default"), rs.getString("extra")));

        assertEquals(List.of("idRol", "NombreRol", "descripcion", "tipo_rol", "activo"),
                columnas.stream().map(Columna::nombre).toList());
        assertEquals("auto_increment", columna(columnas, "idRol").extra());
        assertEquals("NO", columna(columnas, "NombreRol").nullable());
        assertEquals("YES", columna(columnas, "descripcion").nullable());
        assertEquals("INTERNO", columna(columnas, "tipo_rol").valorDefault());
        assertEquals("1", columna(columnas, "activo").valorDefault());

        columnas.forEach(c -> System.out.printf("roles.%s | %s | nullable=%s | default=%s | extra=%s%n",
                c.nombre(), c.tipo(), c.nullable(), c.valorDefault(), c.extra()));
    }

    private Columna columna(List<Columna> columnas, String nombre) {
        return columnas.stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    private record Columna(String nombre, String tipo, String nullable, String valorDefault, String extra) {}
}
