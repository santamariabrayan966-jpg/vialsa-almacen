package com.dentia.service.interno;

import com.dentia.dao.Jdbc.JdbcCuotaVentaDao;
import com.dentia.dao.Jdbc.JdbcVentaDao;
import com.dentia.model.interno.CuotaVenta;
import com.dentia.model.interno.PagoCuota;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CuotaVentaService {

    private final JdbcCuotaVentaDao cuotaVentaDao;
    private final PagoCuotaService pagoCuotaService;
    private final JdbcVentaDao ventaDao;

    public CuotaVentaService(
            JdbcCuotaVentaDao cuotaVentaDao,
            PagoCuotaService pagoCuotaService,
            JdbcVentaDao ventaDao
    ) {
        this.cuotaVentaDao = cuotaVentaDao;
        this.pagoCuotaService = pagoCuotaService;
        this.ventaDao = ventaDao;
    }

    public List<CuotaVenta> listarPorVenta(Integer idVenta) {
        return cuotaVentaDao.listarPorVenta(idVenta);
    }

    public CuotaVenta buscarPorId(Integer idCuotaVenta) {
        return cuotaVentaDao.buscarPorId(idCuotaVenta);
    }

    public void registrarCuotas(Integer idVenta,
                                List<Integer> nroCuota,
                                List<String> fechaVencimiento,
                                List<BigDecimal> montoCuota) {

        cuotaVentaDao.registrarCuotas(
                idVenta,
                nroCuota,
                fechaVencimiento,
                montoCuota
        );
    }

    public void registrarPagoCuota(CuotaVenta cuota, PagoCuota pago) {

        if (pago.getMontoPagado() == null
                || pago.getMontoPagado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        }

        if ("PAGADA".equalsIgnoreCase(cuota.getEstado())) {
            throw new IllegalArgumentException("La cuota ya fue pagada.");
        }

        if (pago.getMontoPagado().compareTo(cuota.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException("El pago supera el saldo pendiente.");
        }

        pagoCuotaService.registrar(pago);

        cuota.setMontoPagado(cuota.getMontoPagado().add(pago.getMontoPagado()));
        cuota.setSaldoPendiente(cuota.getSaldoPendiente().subtract(pago.getMontoPagado()));

        if (cuota.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            cuota.setEstado("PAGADA");
            cuota.setFechaPago(LocalDate.now());
            cuota.setMetodoPago(pago.getMetodoPago());
        } else {
            cuota.setEstado("PARCIAL");
        }

        cuotaVentaDao.actualizarPagoCuota(cuota);

        ventaDao.disminuirDeudaCredito(
                cuota.getIdVenta(),
                pago.getMontoPagado()
        );

        boolean todasPagadas = cuotaVentaDao
                .listarPorVenta(cuota.getIdVenta())
                .stream()
                .allMatch(c -> "PAGADA".equalsIgnoreCase(c.getEstado()));

        if (todasPagadas) {
            ventaDao.marcarCreditoPagado(cuota.getIdVenta());
        }
    }
}