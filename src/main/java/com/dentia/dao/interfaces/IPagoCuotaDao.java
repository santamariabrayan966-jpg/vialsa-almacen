package com.dentia.dao.interfaces;

import com.dentia.model.interno.PagoCuota;
import java.util.List;

public interface IPagoCuotaDao {

    int registrar(PagoCuota pago);

    List<PagoCuota> listarPorCuota(Integer idCuotaVenta);

    List<PagoCuota> listarPorVenta(Integer idVenta);
}
