package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Movimiento;
import java.util.List;
import java.math.BigDecimal;

public interface IMovimientoDao {

    // 📋 Listar todos los movimientos
    List<Movimiento> listar();

    // 💾 Registrar un nuevo movimiento
    int registrar(Movimiento movimiento);

    // 🔍 Buscar ID de usuario por su nombre (usado por el controlador)
    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);

    // 🔍 Buscar movimiento por ID
    Movimiento buscarPorId(Integer id);

    // 📦 Listar movimientos de un producto
    List<Movimiento> listarPorProducto(Integer idProducto);

    // 📊 Obtener stock actual (debe existir en DAO)
    BigDecimal obtenerStockActual(Integer idProducto);
}
