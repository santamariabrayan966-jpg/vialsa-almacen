package com.dentia.service.paciente;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.model.paciente.CuentaPaciente;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientUserDetailsService implements UserDetailsService {

    private final ICuentaPacienteDao cuentaPacienteDao;

    public PatientUserDetailsService(ICuentaPacienteDao cuentaPacienteDao) {
        this.cuentaPacienteDao = cuentaPacienteDao;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        CuentaPaciente cuenta = cuentaPacienteDao.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta de paciente no encontrada."));
        return new User(
                cuenta.getCorreo(),
                cuenta.getPasswordHash(),
                cuenta.isActivo(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"))
        );
    }
}
