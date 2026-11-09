package com.dentia.service.clinica;

import com.dentia.service.agenda.AgendaValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioDentalCatalogoServiceTest {
    @Test
    void usaElIdentificadorDeAgendaYaVinculadoSinCrearOtro() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(7L))).thenReturn(List.of(Map.of(
                "nombre", "Profilaxis dental", "descripcion", "Limpieza",
                "duracion", 30, "tipoId", 19L)));
        assertEquals(19L, new ServicioDentalCatalogoService(jdbc).tipoAgendaParaServicio(7L));
    }

    @Test
    void rechazaUnServicioInactivoAntesDeMostrarTurnos() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(7L))).thenReturn(0);
        assertThrows(AgendaValidationException.class,
                () -> new ServicioDentalCatalogoService(jdbc).validarActivo(7L));
    }
}
