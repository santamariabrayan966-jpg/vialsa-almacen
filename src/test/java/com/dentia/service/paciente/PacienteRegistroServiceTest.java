package com.dentia.service.paciente;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.paciente.RegistroPacienteDto;
import com.dentia.model.paciente.CuentaPaciente;
import com.dentia.model.paciente.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacienteRegistroServiceTest {

    private IPacienteDao pacienteDao;
    private ICuentaPacienteDao cuentaDao;
    private PacienteRegistroService service;

    @BeforeEach
    void setUp() {
        pacienteDao = mock(IPacienteDao.class);
        cuentaDao = mock(ICuentaPacienteDao.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("segura123")).thenReturn("hash-bcrypt");
        when(pacienteDao.crear(any(Paciente.class))).thenReturn(41L);
        service = new PacienteRegistroService(pacienteDao, cuentaDao, encoder);
    }

    @Test
    void registraPacienteConDniEnEstructurasSeparadas() {
        service.registrar(dto("CON_DNI", "12345678"));

        ArgumentCaptor<Paciente> paciente = ArgumentCaptor.forClass(Paciente.class);
        ArgumentCaptor<CuentaPaciente> cuenta = ArgumentCaptor.forClass(CuentaPaciente.class);
        verify(pacienteDao).crear(paciente.capture());
        verify(cuentaDao).crear(cuenta.capture());
        assertTrue("12345678".equals(paciente.getValue().getDni()));
        assertTrue(cuenta.getValue().getIdPaciente() == 41L);
        assertTrue("hash-bcrypt".equals(cuenta.getValue().getPasswordHash()));
    }

    @Test
    void registraPacienteSinDniComoNull() {
        service.registrar(dto("SIN_DNI", ""));

        ArgumentCaptor<Paciente> paciente = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteDao).crear(paciente.capture());
        assertNull(paciente.getValue().getDni());
        verify(pacienteDao, never()).existeDni(any());
    }

    @Test
    void rechazaDniDuplicadoAntesDeInsertar() {
        when(pacienteDao.existeDni("12345678")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(dto("CON_DNI", "12345678")));
        verify(pacienteDao, never()).crear(any());
    }

    private RegistroPacienteDto dto(String tipo, String dni) {
        RegistroPacienteDto dto = new RegistroPacienteDto();
        dto.setTipoRegistro(tipo);
        dto.setDni(dni);
        dto.setNombres("Ana");
        dto.setApellidos("Torres");
        dto.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        dto.setCelular("999888777");
        dto.setCorreo("ANA@EXAMPLE.COM");
        dto.setPassword("segura123");
        dto.setConfirmarPassword("segura123");
        return dto;
    }
}
