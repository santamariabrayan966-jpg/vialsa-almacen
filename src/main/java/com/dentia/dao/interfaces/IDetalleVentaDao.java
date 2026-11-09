package com.dentia.dao.interfaces;

import com.dentia.model.interno.DetalleVenta;

import java.util.List;
import java.math.BigDecimal;

public interface IDetalleVentaDao {

    List<DetalleVenta> listarPorVenta(int idVenta);

    int registrar(DetalleVenta detalle);

    int registrarDetalleVentaOnline(Integer idVenta, DetalleVenta detalle);

    int actualizarPlanoCorteJson(Integer idDetalleVenta, String planoCorteJson);

    int actualizarPreparacionAutomatica(
            Integer idDetalleVenta,
            boolean requierePreparacion,
            String tipoPreparacion,
            boolean entregarSobrante,
            String observacionPreparacion,
            String cortesJson,
            String planoCorteJson,
            BigDecimal cantidadDescontadaStock
    );

    int eliminarPorVenta(int idVenta);
}