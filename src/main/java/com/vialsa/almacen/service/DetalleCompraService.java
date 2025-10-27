package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IDetalleCompraDao;
import com.vialsa.almacen.model.DetalleCompra;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DetalleCompraService {

    private final IDetalleCompraDao dao;

    public DetalleCompraService(IDetalleCompraDao dao) {
        this.dao = dao;
    }

    public boolean registrar(DetalleCompra detalle) {
        return dao.registrar(detalle) > 0;
    }

    public List<DetalleCompra> listarPorCompra(int idCompra) {
        return dao.listarPorCompra(idCompra);
    }

    public boolean eliminarPorCompra(int idCompra) {
        return dao.eliminarPorCompra(idCompra);
    }
}
