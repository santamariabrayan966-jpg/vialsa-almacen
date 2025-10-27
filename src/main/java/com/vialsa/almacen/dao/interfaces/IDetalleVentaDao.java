package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.DetalleVenta;
import java.util.List;

public interface IDetalleVentaDao {
    List<DetalleVenta> listarPorVenta(int idVenta);
    int registrar(DetalleVenta detalle);
    int eliminarPorVenta(int idVenta);
}
