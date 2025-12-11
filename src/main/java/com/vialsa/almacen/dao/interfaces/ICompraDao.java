package com.vialsa.almacen.dao.interfaces;

import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface ICompraDao {

    List<Compra> listar();

    int registrarYObtenerId(Compra c);

    Compra buscarPorId(int idCompra);

    List<DetalleCompra> listarPorCompra(int idCompra);

    Integer obtenerIdUsuarioPorNombre(String nombreUsuario);

    int actualizarEstado(int idCompra, String estado);

    int actualizarCabecera(Compra c);

    int eliminarSiBorrador(int idCompra);

    String obtenerUltimoNumeroOrden();

    int actualizarDeuda(int idCompra, BigDecimal nuevaDeuda);

    // ✔ SÍ DEBEN ESTAR ESTOS DOS
    String obtenerUltimaSeriePorTipo(String tipo);
    String obtenerUltimoNumeroPorTipo(String tipo);
}
