package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.ICompraDao;
import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompraService {

    private final ICompraDao compraDao;

    public CompraService(ICompraDao compraDao) {
        this.compraDao = compraDao;
    }

    // 📋 Listar todas las compras
    public List<Compra> listar() {
        return compraDao.listar();
    }

    // 💾 Registrar compra simple
    public boolean registrar(Compra compra) {
        return compraDao.registrar(compra) > 0;
    }

    // 💾 Registrar compra y devolver el ID generado
    public int registrarYObtenerId(Compra compra) {
        return compraDao.registrarYObtenerId(compra);
    }

    // 🔍 Buscar compra por ID
    public Compra buscarPorId(int idCompra) {
        return compraDao.buscarPorId(idCompra);
    }

    // 🔍 Listar los detalles de una compra específica
    public List<DetalleCompra> listarDetallesPorCompra(int idCompra) {
        return compraDao.listarPorCompra(idCompra);
    }

    // 🔍 Obtener ID de usuario por su nombre
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        return compraDao.obtenerIdUsuarioPorNombre(nombreUsuario);
    }
}
