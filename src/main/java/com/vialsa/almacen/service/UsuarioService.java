package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// PARA GOOGLE
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioDao usuarioDao;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioDao usuarioDao, PasswordEncoder passwordEncoder) {
        this.usuarioDao = usuarioDao;
        this.passwordEncoder = passwordEncoder;
    }

    // ======================================================
    // LISTAR SOLO USUARIOS INTERNOS
    // ======================================================
    public List<Usuario> listarUsuariosInternos() {
        return usuarioDao.listarUsuariosInternos();
    }

    // ======================================================
    // BUSCAR POR ID
    // ======================================================
    public Usuario buscarPorId(Integer id) {
        return id == null ? null : usuarioDao.obtenerPorId(id);
    }

    // ======================================================
    // VALIDACIONES
    // ======================================================
    private void validarCamposObligatorios(Usuario u) {

        if (u.getNombreUsuario() == null || u.getNombreUsuario().isBlank())
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");

        if (u.getNombres() == null || u.getNombres().isBlank())
            throw new IllegalArgumentException("Los nombres son obligatorios.");

        if (u.getApellidos() == null || u.getApellidos().isBlank())
            throw new IllegalArgumentException("Los apellidos son obligatorios.");

        if (u.getIdRol() == null)
            throw new IllegalArgumentException("Debe seleccionar un rol.");

        // 🚫 NO PERMITIR ROL CLIENTE (8)
        if (u.getIdRol() == 8)
            throw new IllegalArgumentException("No puedes asignar el rol CLIENTE desde el panel de usuarios.");

        // PERMITIR correo vacío, solo validar si tiene algo
        if (u.getCorreo() != null && !u.getCorreo().isBlank()) {
            if (!u.getCorreo().matches("^[\\w\\.-]+@[\\w\\.-]+\\.[A-Za-z]{2,}$"))
                throw new IllegalArgumentException("Correo electrónico inválido.");
        }

    }

    private void validarDuplicados(Usuario u, boolean esNuevo) {

        usuarioDao.obtenerPorNombre(u.getNombreUsuario()).ifPresent(existente -> {
            if (esNuevo || !existente.getIdUsuario().equals(u.getIdUsuario()))
                throw new IllegalArgumentException("El usuario '" + u.getNombreUsuario() + "' ya existe.");
        });

        if (u.getCorreo() != null && !u.getCorreo().isBlank()) {
            usuarioDao.findByCorreo(u.getCorreo()).ifPresent(existente -> {
                if (esNuevo || !existente.getIdUsuario().equals(u.getIdUsuario()))
                    throw new IllegalArgumentException("El correo '" + u.getCorreo() + "' ya está registrado.");
            });
        }
    }

    // ======================================================
    // GUARDAR USUARIO INTERNO
    // ======================================================
    public void guardarUsuarioInterno(Usuario usuario) {

        validarCamposObligatorios(usuario);
        validarDuplicados(usuario, true);

        if (usuario.getContrasena() == null || usuario.getContrasena().isBlank())
            throw new IllegalArgumentException("La contraseña es obligatoria.");

        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));

        // ⭐ AQUI LA LINEA QUE FALTABA ⭐
        if (usuario.getIdTipoDocumento() == null) {
            usuario.setIdTipoDocumento(1); // 1 = DNI
        }

        usuarioDao.crear(usuario);
    }


    // ======================================================
    // ACTUALIZAR USUARIO INTERNO
    // ======================================================
    public void actualizarUsuarioInterno(
            Usuario usuario,
            String nuevaContrasena,
            String contrasenaActual
    ) {

        validarCamposObligatorios(usuario);
        validarDuplicados(usuario, false);

        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {

            if (!nuevaContrasena.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$"))
                throw new IllegalArgumentException("La contraseña no cumple requisitos de seguridad.");

            usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));

        } else {
            usuario.setContrasena(contrasenaActual);
        }

        usuarioDao.actualizar(usuario);
    }

    // ======================================================
    // ELIMINAR
    // ======================================================
    public void eliminar(Integer id) {
        if (id != null) usuarioDao.eliminarUsuario(id);
    }

    // ======================================================
    // PERFIL ACTUAL
    // ======================================================
    public Usuario obtenerUsuarioActual() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails u)
            return usuarioDao.obtenerPorNombre(u.getUsername()).orElse(null);

        if (principal instanceof OidcUser oidc)
            return usuarioDao.findByCorreo(oidc.getEmail()).orElse(null);

        if (principal instanceof OAuth2User oauth2)
            return usuarioDao.findByCorreo(String.valueOf(oauth2.getAttribute("email"))).orElse(null);

        if (principal instanceof String s)
            return usuarioDao.obtenerPorNombre(s).orElse(null);

        return null;
    }

    // ======================================================
    // CAMBIAR ESTADO DE UN USUARIO
    // ======================================================
    public void cambiarEstadoActivo(Integer id, boolean activo) {
        if (id != null) usuarioDao.cambiarEstadoActivo(id, activo);
    }

    // ======================================================
    // 🚀 CAMBIAR ESTADO ACTIVO A TODOS LOS USUARIOS DE UN ROL
    // ======================================================
    public void cambiarEstadoActivoPorRol(Integer idRol, boolean activo) {
        usuarioDao.cambiarEstadoActivoPorRol(idRol, activo);
    }

    // ======================================================
    // ACTUALIZAR PERFIL DEL USUARIO LOGUEADO
    // ======================================================
    public void actualizarPerfil(Usuario usuario) {

        if (usuario.getIdUsuario() == null)
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");

        validarCamposObligatorios(usuario);
        validarDuplicados(usuario, false);

        if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        }

        usuarioDao.actualizarPerfil(usuario);
    }
}
