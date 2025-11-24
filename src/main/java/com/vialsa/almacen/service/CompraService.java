package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.ICompraDao;
import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompraService {

    private final ICompraDao compraDao;

    public CompraService(ICompraDao compraDao) {
        this.compraDao = compraDao;
    }

    public List<Compra> listar() {
        return compraDao.listar();
    }

    public int registrarYObtenerId(Compra compra) {
        prepararCompra(compra);
        return compraDao.registrarYObtenerId(compra);
    }

    public Compra buscarPorId(int idCompra) {
        return compraDao.buscarPorId(idCompra);
    }

    public List<DetalleCompra> listarDetallesPorCompra(int idCompra) {
        return compraDao.listarPorCompra(idCompra);
    }

    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        return compraDao.obtenerIdUsuarioPorNombre(nombreUsuario);
    }

    // Anular compra
    public boolean anularCompra(int idCompra) {
        return compraDao.actualizarEstado(idCompra, "ANULADA") > 0;
    }

    // Actualizar SOLO cabecera
    public boolean actualizarCabecera(Compra compra) {
        return compraDao.actualizarCabecera(compra) > 0;
    }

    // Eliminar solo si está en BORRADOR
    public boolean eliminarSiBorrador(int idCompra) {
        return compraDao.eliminarSiBorrador(idCompra) > 0;
    }

    // Confirmar compra BORRADOR => REGISTRADA
    public boolean confirmarCompra(int idCompra) {
        return compraDao.actualizarEstado(idCompra, "REGISTRADA") > 0;
    }
    public interface DetalleCompraService {

        void registrar(DetalleCompra detalle);

        List<DetalleCompra> listarPorCompra(int idCompra);

        // 🔴 NUEVO: para poder reemplazar el detalle al editar
        void eliminarPorCompra(int idCompra);
    }


    private void prepararCompra(Compra compra) {
        if (compra.getFechaCompra() == null) {
            compra.setFechaCompra(LocalDateTime.now());
        }
        if (compra.getFechaEmision() == null) {
            compra.setFechaEmision(LocalDateTime.now());
        }

        // 🔹 Porcentaje IGV por defecto (elige 0 o 18 según tu negocio)
        if (compra.getPorcentajeIgv() == null) {
            // compra.setPorcentajeIgv(new BigDecimal("18.00")); // si siempre quieres 18%
            compra.setPorcentajeIgv(BigDecimal.ZERO);           // o 0 si lo manejas desde el front
        }

        if (compra.getTotalCompra() == null) {
            compra.setTotalCompra(BigDecimal.ZERO);
        }
        if (compra.getSubtotal() == null) {
            compra.setSubtotal(BigDecimal.ZERO);
        }
        if (compra.getMontoIgv() == null) {
            compra.setMontoIgv(BigDecimal.ZERO);
        }
        if (compra.getEstado() == null || compra.getEstado().isBlank()) {
            compra.setEstado("REGISTRADA");
        }
    }

}
