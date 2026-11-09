package com.dentia.service.paciente;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.paciente.RegistroPacienteDto;
import com.dentia.model.paciente.CuentaPaciente;
import com.dentia.model.paciente.Paciente;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class PacienteRegistroService {

    private final IPacienteDao pacienteDao;
    private final ICuentaPacienteDao cuentaPacienteDao;
    private final PasswordEncoder passwordEncoder;

    public PacienteRegistroService(IPacienteDao pacienteDao,
                                   ICuentaPacienteDao cuentaPacienteDao,
                                   PasswordEncoder passwordEncoder) {
        this.pacienteDao = pacienteDao;
        this.cuentaPacienteDao = cuentaPacienteDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registrar(RegistroPacienteDto dto) {
        String dni = normalizarDni(dto);
        String correo = dto.getCorreo().trim().toLowerCase(Locale.ROOT);

        if (!dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }
        if (dni != null && pacienteDao.existeDni(dni)) {
            throw new IllegalArgumentException("Ya existe un paciente registrado con ese DNI.");
        }
        if (cuentaPacienteDao.existeCorreo(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }

        Paciente paciente = new Paciente();
        paciente.setDni(dni);
        paciente.setNombres(dto.getNombres().trim());
        paciente.setApellidos(dto.getApellidos().trim());
        paciente.setFechaNacimiento(dto.getFechaNacimiento());
        paciente.setCelular(dto.getCelular().trim());
        paciente.setActivo(true);

        try {
            long idPaciente = pacienteDao.crear(paciente);

            CuentaPaciente cuenta = new CuentaPaciente();
            cuenta.setIdPaciente(idPaciente);
            cuenta.setCorreo(correo);
            cuenta.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            cuenta.setActivo(true);
            cuentaPacienteDao.crear(cuenta);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("El DNI o el correo ya se encuentran registrados.", ex);
        }
    }

    private String normalizarDni(RegistroPacienteDto dto) {
        if ("SIN_DNI".equals(dto.getTipoRegistro())) return null;
        String dni = dto.getDni() == null ? "" : dto.getDni().trim();
        if (!dni.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 digitos.");
        }
        return dni;
    }
}
