package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.ICuotaCompraDao;
import com.vialsa.almacen.dao.interfaces.ICompraDao;
import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.CuotaCompra;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CuotaCompraService {

    private final ICuotaCompraDao cuotaDao;
    private final ICompraDao compraDao;

    public CuotaCompraService(ICuotaCompraDao cuotaDao, ICompraDao compraDao) {
        this.cuotaDao = cuotaDao;
        this.compraDao = compraDao;
    }

    public void crearCuota(int idCompra, int nro, LocalDate fecha, BigDecimal monto) {
        CuotaCompra c = new CuotaCompra();
        c.setIdCompra(idCompra);
        c.setNumeroCuota(nro);
        c.setFechaVencimiento(fecha);
        c.setMontoCuota(monto);
        c.setEstado("PENDIENTE");

        cuotaDao.registrar(c);
    }

    public List<CuotaCompra> listarPorCompra(int idCompra) {
        return cuotaDao.listarPorCompra(idCompra);
    }

    /**
     * Pagar una cuota:
     * 1. Marca cuota pagada
     * 2. Recalcula deuda total de la compra
     * 3. Si deuda = 0 → marca compra como PAGADA
     */
    public boolean pagarCuota(int idCuota) {

        CuotaCompra cuota = cuotaDao.buscarPorId(idCuota);

        if (cuota == null) {
            return false;
        }

        int idCompra = cuota.getIdCompra();

        // 1️⃣ Marcar cuota como pagada
        cuotaDao.marcarComoPagada(idCuota);

        // 2️⃣ Recalcular deuda total
        List<CuotaCompra> cuotas = cuotaDao.listarPorCompra(idCompra);

        BigDecimal nuevaDeuda = cuotas.stream()
                .filter(c -> !"PAGADA".equals(c.getEstado()))
                .map(CuotaCompra::getMontoCuota)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3️⃣ Actualizar deuda en la compra
        Compra compra = compraDao.buscarPorId(idCompra);
        compra.setDeuda(nuevaDeuda);

        compraDao.actualizarCabecera(compra);

        // 4️⃣ Si ya no hay deuda → marcar compra como PAGADA
        if (nuevaDeuda.compareTo(BigDecimal.ZERO) == 0) {
            compraDao.actualizarEstado(idCompra, "PAGADA");
        }

        return true;
    }

    public void eliminarPorCompra(int idCompra) {
        cuotaDao.eliminarPorCompra(idCompra);
    }
}
