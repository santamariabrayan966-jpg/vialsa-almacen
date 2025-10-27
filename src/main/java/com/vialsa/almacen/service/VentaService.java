package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IVentaDao;
import com.vialsa.almacen.model.Venta;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VentaService {

    private final IVentaDao dao;

    public VentaService(IVentaDao dao) {
        this.dao = dao;
    }

    // 📋 Listar todas las ventas
    public List<Venta> listar() {
        return dao.listar();
    }

    // 💾 Registrar venta (sin devolver ID)
    public boolean registrar(Venta venta) {
        return dao.registrar(venta) > 0;
    }

    // 💾 Registrar venta y devolver ID generado
    public int registrarYObtenerId(Venta venta) {
        return dao.registrarYObtenerId(venta);
    }

    // 🔍 Obtener ID de usuario por nombre
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        return dao.obtenerIdUsuarioPorNombre(nombreUsuario);
    }

    // 🔍 Buscar venta por ID
    public Venta buscarPorId(int idVenta) {
        return dao.buscarPorId(idVenta);
    }
}
