package com.dentia.dao.interfaces;

import com.dentia.model.interno.Compra;
import com.dentia.model.interno.DetalleCompra;

import java.math.BigDecimal;
import java.util.List;


public interface ICompraDao {

    List<Compra> listar();

    List<Compra> listarPorSede(Integer idSede);

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

    void actualizarCostoPromedioProducto(
            int idProducto,
            BigDecimal cantidadComprada,
            BigDecimal costoUnitarioCompraPen
    );

    boolean existeComprobanteProveedor(
            Integer idProveedor,
            String tipoComprobante,
            String serie,
            String numero,
            Integer idCompraExcluir
    );
}
