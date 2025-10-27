package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Movimiento;
import java.util.List;

public interface IMovimientoDao {

    // 📋 Listar todos los movimientos
    List<Movimiento> listar();

    // 💾 Registrar un nuevo movimiento
    int registrar(Movimiento movimiento);

    // 🔍 Buscar ID de usuario por su nombre (usado por el controlador)
    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);
}
