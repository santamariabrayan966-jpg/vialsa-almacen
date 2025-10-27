package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IMovimientoDao;
import com.vialsa.almacen.model.Movimiento;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovimientoService {

    private final IMovimientoDao dao;

    // ✅ Inyección por constructor
    public MovimientoService(IMovimientoDao dao) {
        this.dao = dao;
    }

    // 📋 Listar todos los movimientos
    public List<Movimiento> listar() {
        return dao.listar();
    }

    // 💾 Registrar un movimiento (retorna true si se insertó correctamente)
    public boolean registrar(Movimiento movimiento) {
        try {
            return dao.registrar(movimiento) > 0;
        } catch (Exception e) {
            System.err.println("❌ Error al registrar movimiento: " + e.getMessage());
            return false;
        }
    }

    // 🔍 Obtener ID del usuario por nombre (usado en el controlador)
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        try {
            return Optional.ofNullable(dao.obtenerIdUsuarioPorNombre(nombreUsuario))
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró el usuario con nombre: " + nombreUsuario));
        } catch (Exception e) {
            System.err.println("⚠️ Error al buscar ID de usuario: " + e.getMessage());
            return null;
        }
    }
}
