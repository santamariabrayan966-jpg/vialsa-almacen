package com.dentia.dao;

import com.dentia.dao.Jdbc.JdbcRolDao;
import com.dentia.model.interno.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("integration")
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcRolDao.class)
class JdbcRolDaoIntegrationTest {
    @Autowired JdbcRolDao dao;
    @Autowired JdbcTemplate jdbc;

    @Test void insertRealGeneraIdYValoresInicialesEnDentia() {
        String nombre = "TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Rol rol = new Rol(); rol.setNombreRol(nombre); rol.setDescripcion("Gestion de recepcion");

        int id = dao.crearRol(rol);

        assertTrue(id > 0);
        Rol guardado = jdbc.queryForObject("SELECT idRol,NombreRol nombreRol,descripcion,activo FROM roles WHERE idRol=?",
                new org.springframework.jdbc.core.BeanPropertyRowMapper<>(Rol.class), id);
        assertNotNull(guardado); assertEquals(nombre, guardado.getNombreRol());
        assertEquals("Gestion de recepcion", guardado.getDescripcion()); assertTrue(guardado.getActivo());
    }
}
