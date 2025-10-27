package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioDao usuarioDao;

    // 📋 Listar todos los usuarios
    public List<Usuario> listarUsuarios() {
        return usuarioDao.listarTodos();
    }

    // 🔍 Buscar usuario por ID
    public Usuario buscarPorId(int id) {
        return usuarioDao.obtenerPorId(id);
    }

    // 💾 Crear nuevo usuario
    public void guardar(Usuario usuario) {
        usuarioDao.crearUsuario(usuario.getNombreUsuario(), usuario.getContrasena(), usuario.getIdRol());
    }

    // ✏️ Actualizar el rol del usuario
    public void actualizar(Usuario usuario) {
        usuarioDao.actualizarRol(usuario.getIdUsuario(), usuario.getIdRol());
    }

    // 🗑️ Eliminar usuario
    public void eliminar(int id) {
        usuarioDao.eliminarUsuario(id);
    }
}
