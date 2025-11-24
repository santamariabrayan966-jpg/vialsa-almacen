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

        log.info(">>> BUSCANDO USUARIO POR NOMBRE O CORREO: {}", username);

        // 1) Buscar por NombreUsuario
        Usuario u = usuarioDao.findByNombreUsuario(username)
                // 2) Si no lo encuentra, buscar por Correo
                .or(() -> usuarioDao.findByCorreo(username))
                .orElseThrow(() -> {
                    log.warn(">>> Usuario NO ENCONTRADO (usuario/correo): {}", username);
                    return new UsernameNotFoundException("Usuario no encontrado");
                });

        log.info(">>> USUARIO ENCONTRADO: idUsuario={}, nombreUsuario={}, correo={}",
                u.getIdUsuario(), u.getNombreUsuario(), u.getCorreo());

        // Validar si el usuario está activo (campo 'activo' en usuarios)
        if (u.getActivo() != null && !u.getActivo()) {
            log.warn(">>> Usuario {} inactivo. Lanzando DisabledException", username);
            throw new DisabledException("USUARIO_INACTIVO");
        }

        int idRol = u.getIdRol(); // int (no null)

        // Validar si el rol está activo
        Boolean rolActivo = rolDao.esRolActivo(idRol);
        log.info(">>> ROL {} ACTIVO? {}", idRol, rolActivo);

        if (Boolean.FALSE.equals(rolActivo)) {
            log.warn(">>> Rol inactivo o inexistente para usuario {}. Lanzando DisabledException(ROL_INACTIVO)", username);
            throw new DisabledException("ROL_INACTIVO");
        }

        // 🔹 Mapear roles de acuerdo a tu tabla 'roles'
        // Asegúrate de que el id de CLIENTE coincida con tu BD (en tu dump es 8)
        // Obtener nombre del rol desde la BD
        String nombreRol = rolDao.obtenerNombreRol(idRol);

        if (nombreRol == null || nombreRol.isBlank()) {
            nombreRol = "USER"; // fallback mínimo
        }

// SPRING NECESITA EL PREFIJO ROLE_
        String role = "ROLE_" + nombreRol.toUpperCase().trim().replace(" ", "_");

        log.info(">>> Asignando authority: {}", role);


        log.info(">>> Asignando authority: {}", role);

        // Si NombreUsuario es null (por registro vía correo o Google), usamos el correo
        String usernameSpring = (u.getNombreUsuario() != null && !u.getNombreUsuario().isBlank())
                ? u.getNombreUsuario()
                : u.getCorreo();

        return new User(
                usernameSpring,          // lo que quedará como principal en el SecurityContext
                u.getContrasena(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
