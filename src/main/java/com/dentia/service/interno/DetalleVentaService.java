package com.dentia.service.interno;

import com.dentia.dao.interfaces.IDetalleVentaDao;
import com.dentia.model.interno.DetalleVenta;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DetalleVentaService {

    private final IDetalleVentaDao dao;

    // ✅ Inyección de dependencias por constructor
    public DetalleVentaService(IDetalleVentaDao dao) {
        this.dao = dao;
    }

    // 📋 Listar detalles de una venta específica
    public List<DetalleVenta> listarPorVenta(int idVenta) {
        return dao.listarPorVenta(idVenta);
    }

    // 💾 Registrar un detalle (línea de producto) dentro de una venta
    public boolean registrar(DetalleVenta detalle) {
        return dao.registrar(detalle) > 0;
    }

    // ❌ Eliminar todos los detalles asociados a una venta (por si se anula)
    public boolean eliminarPorVenta(int idVenta) {
        return dao.eliminarPorVenta(idVenta) > 0;
    }
}
