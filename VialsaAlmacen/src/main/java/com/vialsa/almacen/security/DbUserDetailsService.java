package com.vialsa.almacen.security;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
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

    private final UsuarioDao usuarioDao;

    // ? Evitamos el ciclo agregando @Lazy en el constructor
    public DbUserDetailsService(@Lazy UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioDao.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String role = switch (u.getIdRol() == null ? 0 : u.getIdRol()) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_VENDEDOR";
            case 3 -> "ROLE_ALMACENERO";
            default -> "ROLE_USER";
        };

        return new User(
                u.getNombreUsuario(),
                u.getContrasena(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
