package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class UsuarioServiceCliente {

    private final UsuarioDao usuarioDao;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioServiceCliente(UsuarioDao usuarioDao,
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService) {
        this.usuarioDao = usuarioDao;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ============================================================
    // VALIDACIONES
    // ============================================================

    public boolean existeCorreo(String correo) {
        return usuarioDao.existeCorreo(correo);
    }

    public boolean existeDocumento(String documento) {
        return usuarioDao.existeDocumento(documento);
    }

    public Usuario buscarPorCorreo(String correo) {
        return usuarioDao.findByCorreo(correo).orElse(null);
    }

    // ============================================================
    // REGISTRO NORMAL (cliente escribe su contraseña)
    // ============================================================

    public Usuario registrarNuevoClienteConPassword(Usuario u, String passwordPlano) {

        String passwordEncriptada = passwordEncoder.encode(passwordPlano);
        u.setContrasena(passwordEncriptada);

        // ❌ YA NO FORZAMOS CLIENTE
        // u.setIdRol(8);

        u.setActivo(true);
        u.setNombreUsuario(u.getCorreo());

        usuarioDao.crear(u);

        return usuarioDao.findByCorreo(u.getCorreo())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el usuario recién creado"));
    }

    // ============================================================
    // REGISTRO CON GOOGLE
    // ============================================================

    public Usuario registrarNuevoCliente(Usuario u) {

        String passwordPlano = generarPasswordSegura();
        String passwordEncriptada = passwordEncoder.encode(passwordPlano);

        u.setContrasena(passwordEncriptada);

        // ❌ YA NO FORZAMOS CLIENTE
        // u.setIdRol(8);

        u.setActivo(true);
        u.setNombreUsuario(u.getCorreo());

        usuarioDao.crear(u);

        Usuario creado = usuarioDao.findByCorreo(u.getCorreo())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el usuario recién creado"));

        String html = """
            <h2>Bienvenido a VIALSA Tienda</h2>
            <p>Hola, %s</p>
            <p>Hemos creado una cuenta vinculada a tu Google.</p>
            <p><b>Usuario:</b> %s</p>
            <p><b>Contraseña temporal:</b> %s</p>
            """
                .formatted(creado.getNombres(), creado.getCorreo(), passwordPlano);

        emailService.enviarCorreo(
                creado.getCorreo(),
                "Cuenta creada con Google - VIALSA",
                html
        );

        return creado;
    }

    public Usuario registrarClienteOAuth2(Usuario u) {
        return registrarNuevoCliente(u);
    }

    // ============================================================
    // PASSWORD ALEATORIA
    // ============================================================

    private String generarPasswordSegura() {
        String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String numeros = "0123456789";
        String simbolos = "!@#$%&*";

        String all = letras + numeros + simbolos;
        SecureRandom random = new SecureRandom();
        StringBuilder pass = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            pass.append(all.charAt(random.nextInt(all.length())));
        }

        return pass.toString();
    }
}
