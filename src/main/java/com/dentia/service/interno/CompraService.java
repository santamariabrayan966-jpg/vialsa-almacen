package com.dentia.service.interno;

import com.dentia.dao.interfaces.ICompraDao;
import com.dentia.model.interno.Compra;
import com.dentia.model.interno.DetalleCompra;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;


@Service
public class CompraService {

    private final ICompraDao compraDao;

    public CompraService(ICompraDao compraDao) {
        this.compraDao = compraDao;
    }

    public List<Compra> listar() {
        return compraDao.listar();
    }

    public List<Compra> listarPorSede(Integer idSede) {
        if (idSede == null || idSede <= 0) {
            return List.of();
        }

        return compraDao.listarPorSede(idSede);
    }

    public int registrarYObtenerId(Compra compra) {

        if (compra.getNroOrdenCompra() == null || compra.getNroOrdenCompra().isBlank()) {
            compra.setNroOrdenCompra(generarNumeroOrdenCompra());
        }

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

    public boolean anularCompra(int idCompra) {
        Compra compra = compraDao.buscarPorId(idCompra);

        if (compra == null) {
            return false;
        }

        String estado = compra.getEstado();

        if ("RECIBIDA_PARCIAL".equalsIgnoreCase(estado)
                || "RECIBIDA_COMPLETA".equalsIgnoreCase(estado)) {
            return false;
        }

        return compraDao.actualizarEstado(idCompra, "ANULADA") > 0;
    }

    public boolean actualizarCabecera(Compra compra) {
        return compraDao.actualizarCabecera(compra) > 0;
    }

    public boolean eliminarSiBorrador(int idCompra) {
        return compraDao.eliminarSiBorrador(idCompra) > 0;
    }

    public boolean confirmarCompra(int idCompra) {
        Compra compra = compraDao.buscarPorId(idCompra);

        if (compra == null) {
            return false;
        }

        if ("INMEDIATA".equalsIgnoreCase(compra.getTipoRecepcion())) {
            return compraDao.actualizarEstado(idCompra, "RECIBIDA_COMPLETA") > 0;
        }

        return compraDao.actualizarEstado(idCompra, "REGISTRADA") > 0;
    }

    // 👉 NUEVO: actualizar deuda desde cuota
    public boolean actualizarDeuda(int idCompra, BigDecimal nuevaDeuda) {
        Compra compra = compraDao.buscarPorId(idCompra);

        if (compra == null) return false;

        compra.setDeuda(nuevaDeuda);
        return compraDao.actualizarCabecera(compra) > 0;
    }

    private void prepararCompra(Compra compra) {
        if (compra.getFechaCompra() == null)
            compra.setFechaCompra(LocalDateTime.now());

        if (compra.getFechaEmision() == null)
            compra.setFechaEmision(LocalDate.now());   // ✔ correcto

        if (compra.getPorcentajeIgv() == null)
            compra.setPorcentajeIgv(BigDecimal.ZERO);

        if (compra.getSubtotal() == null)
            compra.setSubtotal(BigDecimal.ZERO);

        if (compra.getMontoIgv() == null)
            compra.setMontoIgv(BigDecimal.ZERO);

        if (compra.getTotalCompra() == null)
            compra.setTotalCompra(BigDecimal.ZERO);

        if (compra.getEstado() == null || compra.getEstado().isBlank())
            compra.setEstado("PENDIENTE_RECEPCION");
    }

    public String generarNumeroOrdenCompra() {
        String ultimo = compraDao.obtenerUltimoNumeroOrden();

        if (ultimo == null || ultimo.isBlank()) {
            return "OC-0001";
        }

        String numeroStr = ultimo.replace("OC-", "");
        int numero = Integer.parseInt(numeroStr);
        numero++;

        return "OC-" + String.format("%04d", numero);
    }

    public Map<String, String> obtenerSiguienteSerieYNumero(String tipo) {

        Map<String, String> r = new HashMap<>();

        // Últimos valores guardados en BD
        String ultimaSerie  = compraDao.obtenerUltimaSeriePorTipo(tipo);
        String ultimoNumero = compraDao.obtenerUltimoNumeroPorTipo(tipo);

        // Si no existe serie registrada → asignar base correcta
        if (ultimaSerie == null || ultimaSerie.isBlank()) {
            switch (tipo.toUpperCase()) {
                case "FACTURA":
                    ultimaSerie = "F001";
                    break;
                case "BOLETA":
                    ultimaSerie = "B001";
                    break;
                case "N.CREDITO":
                    ultimaSerie = "FC01";  // CORRECTO SUNAT
                    break;
                case "N.DEBITO":
                    ultimaSerie = "FD01";  // CORRECTO SUNAT
                    break;
                default:
                    ultimaSerie = "F001";
            }
        }

        // Si no existe número → empieza en 1
        int siguienteNumero = 1;

        if (ultimoNumero != null && !ultimoNumero.isBlank()) {
            try {
                siguienteNumero = Integer.parseInt(ultimoNumero) + 1;
            } catch (Exception ignored) {}
        }

        // Formato 6 dígitos
        String nuevoNumero = String.format("%06d", siguienteNumero);

        r.put("serie", ultimaSerie);
        r.put("numero", nuevoNumero);

        return r;
    }

    public BigDecimal actualizarCostoPromedioDesdeCompra(Compra compra, DetalleCompra detalle) {

        if (compra == null || detalle == null) {
            return BigDecimal.ZERO;
        }

        if (detalle.getCantidad() == null || detalle.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal costoUnitarioPen = calcularCostoUnitarioCompraPen(compra, detalle);

        if (costoUnitarioPen.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        compraDao.actualizarCostoPromedioProducto(
                detalle.getIdProducto(),
                detalle.getCantidad(),
                costoUnitarioPen
        );

        return costoUnitarioPen;
    }

    private BigDecimal calcularCostoUnitarioCompraPen(Compra compra, DetalleCompra detalle) {

        BigDecimal cantidad = detalle.getCantidad();
        BigDecimal precioUnitario = detalle.getPrecioUnitario();
        BigDecimal descuento = detalle.getDescuento();

        BigDecimal importeLinea = precioUnitario.multiply(cantidad).subtract(descuento);

        if (importeLinea.compareTo(BigDecimal.ZERO) < 0) {
            importeLinea = BigDecimal.ZERO;
        }

        if ("USD".equalsIgnoreCase(compra.getMoneda())) {
            BigDecimal tipoCambio = compra.getTipoCambio() != null
                    ? compra.getTipoCambio()
                    : BigDecimal.ZERO;

            importeLinea = importeLinea.multiply(tipoCambio);
        }

        if (Boolean.TRUE.equals(compra.isIncluyeIgv())) {
            BigDecimal porcentajeIgv = compra.getPorcentajeIgv() != null
                    ? compra.getPorcentajeIgv()
                    : BigDecimal.ZERO;

            BigDecimal tasaIgv = porcentajeIgv.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

            importeLinea = importeLinea.divide(
                    BigDecimal.ONE.add(tasaIgv),
                    6,
                    RoundingMode.HALF_UP
            );
        }

        return importeLinea.divide(cantidad, 6, RoundingMode.HALF_UP);
    }

    public boolean existeComprobanteProveedor(Compra compra) {
        if (compra == null) return false;

        return compraDao.existeComprobanteProveedor(
                compra.getIdProveedor(),
                compra.getTipoComprobante(),
                compra.getSerie(),
                compra.getNumero(),
                compra.getIdCompra()
        );
    }


}
