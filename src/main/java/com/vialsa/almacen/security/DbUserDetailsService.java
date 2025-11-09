package com.vialsa.almacen.security;

import com.vialsa.almacen.dao.Jdbc.JdbcRolDao;
import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class DbUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(DbUserDetailsService.class);

    private final UsuarioDao usuarioDao;
    private final JdbcRolDao rolDao;

    public DbUserDetailsService(@Lazy UsuarioDao usuarioDao,
                                JdbcRolDao rolDao) {
        this.usuarioDao = usuarioDao;
        this.rolDao = rolDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.info(">>> BUSCANDO USUARIO: {}", username);

        Usuario u = usuarioDao.findByNombreUsuario(username)
                .orElseThrow(() -> {
                    log.warn(">>> Usuario NO ENCONTRADO: {}", username);
                    return new UsernameNotFoundException("Usuario no encontrado");
                });

        int idRol = u.getIdRol(); // 👈 es int, nunca null

        log.info(">>> USUARIO ENCONTRADO: idUsuario={}, idRol={}",
                u.getIdUsuario(), idRol);

        // ✅ Consultamos si el rol está activo (false si no existe o está inactivo)
        Boolean rolActivo = rolDao.esRolActivo(idRol);
        log.info(">>> ROL {} ACTIVO? {}", idRol, rolActivo);

        if (Boolean.FALSE.equals(rolActivo)) {
            log.warn(">>> Rol inactivo o inexistente para usuario {}. Lanzando DisabledException(ROL_INACTIVO)", username);
            throw new DisabledException("ROL_INACTIVO");
        }

        String role = switch (idRol) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_VENDEDOR";
            case 3 -> "ROLE_ALMACENERO";
            case 4 -> "ROLE_GERENTE";
            case 5 -> "ROLE_CAJERO";
            case 6 -> "ROLE_SUPERVISOR";
            default -> "ROLE_USER";
        };

        log.info(">>> Asignando authority: {}", role);

        return new User(
                u.getNombreUsuario(),
                u.getContrasena(),
                List.of(new SimpleGrantedAuthority(role))
        );

    }

}
