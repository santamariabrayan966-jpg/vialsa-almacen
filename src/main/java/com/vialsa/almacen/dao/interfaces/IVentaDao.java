package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Venta;
import java.util.List;

public interface IVentaDao {
    List<Venta> listar();
    int registrar(Venta venta);
    int registrarYObtenerId(Venta venta);
    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);
    Venta buscarPorId(int idVenta); // ✅ ESTE método es obligatorio
}
