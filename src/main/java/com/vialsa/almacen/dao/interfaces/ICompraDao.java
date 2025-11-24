package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;

import java.util.List;

public interface ICompraDao {

    List<Compra> listar();

    int registrarYObtenerId(Compra c);

    Compra buscarPorId(int idCompra);

    List<DetalleCompra> listarPorCompra(int idCompra);

    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);

    // cambiar estado de la compra (REGISTRADA, ANULADA, PAGADA, etc.)
    int actualizarEstado(int idCompra, String estado);

    // actualizar SOLO cabecera, sin tocar detalle
    int actualizarCabecera(Compra c);

    // eliminar solo si está en BORRADOR
    int eliminarSiBorrador(int idCompra);
}
