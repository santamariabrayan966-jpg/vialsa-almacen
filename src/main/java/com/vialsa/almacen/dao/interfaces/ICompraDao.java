package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import java.util.List;

public interface ICompraDao {
    List<Compra> listar();
    int registrar(Compra c);
    int registrarYObtenerId(Compra c);
    Compra buscarPorId(int idCompra);
    List<DetalleCompra> listarPorCompra(int idCompra);
    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);
}
