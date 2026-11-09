package com.dentia.service.interno;

import com.dentia.dao.interfaces.IPagoCuotaDao;
import com.dentia.model.interno.PagoCuota;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PagoCuotaService {

    private final IPagoCuotaDao pagoCuotaDao;

    public PagoCuotaService(IPagoCuotaDao pagoCuotaDao) {
        this.pagoCuotaDao = pagoCuotaDao;
    }

    public int registrar(PagoCuota pago) {
        return pagoCuotaDao.registrar(pago);
    }

    public List<PagoCuota> listarPorCuota(Integer idCuotaVenta) {
        return pagoCuotaDao.listarPorCuota(idCuotaVenta);
    }

    public List<PagoCuota> listarPorVenta(Integer idVenta) {
        return pagoCuotaDao.listarPorVenta(idVenta);
    }
}