package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IMovimientoDao;
import com.vialsa.almacen.model.Movimiento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

            // =============== 🧠 NUEVA LÓGICA PROFESIONAL ====================
            // Antes de registrar, debemos calcular stockAntes y stockDespues
            BigDecimal stockActual = dao.obtenerStockActual(movimiento.getIdProducto());

            if (stockActual == null) {
                throw new IllegalStateException("No se pudo obtener el stock actual del producto " +
                        movimiento.getIdProducto());
            }

            movimiento.setStockAntes(stockActual);

            BigDecimal cantidad = movimiento.getCantidad();

            BigDecimal stockDespues;

            if ("ENTRADA".equalsIgnoreCase(movimiento.getTipoMovimiento())) {
                stockDespues = stockActual.add(cantidad);
            } else if ("SALIDA".equalsIgnoreCase(movimiento.getTipoMovimiento())) {
                stockDespues = stockActual.subtract(cantidad);

                if (stockDespues.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("El stock no puede quedar negativo.");
                }
            } else {
                throw new IllegalArgumentException("Tipo de movimiento inválido: " + movimiento.getTipoMovimiento());
            }

            movimiento.setStockDespues(stockDespues);
            // ===============================================================

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
