package com.dentia.service.interno;

import com.dentia.dao.interfaces.ICompraRecepcionDao;
import com.dentia.model.interno.Compra;
import com.dentia.model.interno.CompraRecepcion;
import com.dentia.model.interno.CompraRecepcionDetalle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dentia.model.interno.DetalleCompra;
import java.math.RoundingMode;
import com.dentia.model.interno.Producto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CompraRecepcionService {

    private final ICompraRecepcionDao recepcionDao;
    private final CompraService compraService;
    private final InventarioService inventarioService;
    private final ProductoService productoService;
    private static final Integer ID_UNIDAD_PLANCHA = 4;

    public CompraRecepcionService(
            ICompraRecepcionDao recepcionDao,
            CompraService compraService,
            InventarioService inventarioService,
            ProductoService productoService
    ) {
        this.recepcionDao = recepcionDao;
        this.compraService = compraService;
        this.inventarioService = inventarioService;
        this.productoService = productoService;
    }

    public List<CompraRecepcion> listarPorCompra(Integer idCompra) {
        return recepcionDao.listarPorCompra(idCompra);
    }

    public List<CompraRecepcionDetalle> listarDetallesPorRecepcion(Integer idRecepcion) {
        return recepcionDao.listarDetallesPorRecepcion(idRecepcion);
    }

    public List<CompraRecepcionDetalle> listarPendientesPorCompra(Integer idCompra) {
        return recepcionDao.listarPendientesPorCompra(idCompra);
    }

    public BigDecimal obtenerValorFaltantePendiente(Integer idCompra) {
        if (idCompra == null || idCompra <= 0) {
            return BigDecimal.ZERO;
        }

        List<CompraRecepcionDetalle> pendientes = recepcionDao.listarPendientesPorCompra(idCompra);

        if (pendientes == null || pendientes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return calcularValorFaltante(idCompra, pendientes)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void registrarRecepcion(
            Integer idCompra,
            String nroGuiaRemision,
            LocalDate fechaRecepcion,
            Integer idSede,
            Integer idUsuario,
            String observacion,
            List<Integer> idDetalleCompra,
            List<Integer> idProducto,
            List<Integer> idUnidad,
            List<String> cantidadRecibida
    ) {
        if (idCompra == null || idCompra <= 0) {
            throw new RuntimeException("Compra inválida.");
        }

        Compra compra = compraService.buscarPorId(idCompra);

        if (compra == null) {
            throw new RuntimeException("No se encontró la compra.");
        }

        if ("BORRADOR".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("No puedes recepcionar una proforma. Primero confirma la compra.");
        }

        if ("ANULADA".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("No puedes recepcionar una compra anulada.");
        }

        if ("RECIBIDA_COMPLETA".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("Esta compra ya fue recibida completamente.");
        }

        if ("CERRADA_CON_DIFERENCIA".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("Esta compra fue cerrada con diferencia y ya no puede recepcionarse.");
        }

        if (nroGuiaRemision == null || nroGuiaRemision.trim().isEmpty()) {
            throw new RuntimeException("Debe ingresar el número de guía de remisión.");
        }

        nroGuiaRemision = nroGuiaRemision.trim().toUpperCase();

        if (recepcionDao.existeGuiaEnCompra(idCompra, nroGuiaRemision)) {
            throw new RuntimeException("Esta guía de remisión ya fue registrada para esta compra.");
        }

        if (fechaRecepcion == null) {
            fechaRecepcion = LocalDate.now();
        }

        if (idSede == null || idSede <= 0) {
            throw new RuntimeException("No se encontró la sede de recepción.");
        }

        if (idUsuario == null || idUsuario <= 0) {
            throw new RuntimeException("No se encontró el usuario responsable.");
        }

        if (idDetalleCompra == null || idDetalleCompra.isEmpty()) {
            throw new RuntimeException("Debe registrar al menos un producto recibido.");
        }

        CompraRecepcion recepcion = new CompraRecepcion();
        recepcion.setIdCompra(idCompra);
        recepcion.setNroGuiaRemision(nroGuiaRemision);
        recepcion.setFechaRecepcion(fechaRecepcion);
        recepcion.setIdSede(idSede);
        recepcion.setIdUsuario(idUsuario);
        recepcion.setEstado("REGISTRADA");
        recepcion.setObservacion(observacion);

        int idRecepcion = recepcionDao.registrarRecepcion(recepcion);

        boolean recibioAlgo = false;

        List<CompraRecepcionDetalle> pendientes = recepcionDao.listarPendientesPorCompra(idCompra);
        List<DetalleCompra> detallesCompra = compraService.listarDetallesPorCompra(idCompra);

        for (int i = 0; i < idDetalleCompra.size(); i++) {

            BigDecimal cantidad = parseCantidad(cantidadRecibida, i);

            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Integer detalleId = idDetalleCompra.get(i);
            Integer productoId = idProducto.get(i);
            Integer unidadId = idUnidad.get(i);

            CompraRecepcionDetalle pendiente = buscarPendiente(pendientes, detalleId);

            if (pendiente == null) {
                throw new RuntimeException("El producto seleccionado ya no tiene cantidad pendiente por recibir.");
            }

            if (cantidad.compareTo(pendiente.getCantidadPendiente()) > 0) {
                throw new RuntimeException(
                        "No puedes recibir más de lo pendiente para el producto: "
                                + pendiente.getNombreProducto()
                                + ". Pendiente: "
                                + pendiente.getCantidadPendiente()
                );
            }

            CompraRecepcionDetalle detalle = new CompraRecepcionDetalle();
            detalle.setIdRecepcion(idRecepcion);
            detalle.setIdDetalleCompra(detalleId);
            detalle.setIdProducto(productoId);
            detalle.setIdUnidad(unidadId);
            detalle.setCantidadRecibida(cantidad);

            recepcionDao.registrarDetalle(detalle);

// Actualiza costo promedio recién cuando la mercadería ingresa.
// Si la recepción es parcial, el descuento se reparte proporcionalmente.
            DetalleCompra detalleCosto = construirDetalleCosto(
                    detallesCompra,
                    detalleId,
                    cantidad
            );

            BigDecimal costoUnitarioInventario =
                    compraService.actualizarCostoPromedioDesdeCompra(compra, detalleCosto);

            String documentoReferencia = "G-" + nroGuiaRemision
                    + " | C" + idCompra
                    + " "
                    + compra.getSerie()
                    + "-"
                    + compra.getNumero();

            if (documentoReferencia.length() > 150) {
                documentoReferencia = documentoReferencia.substring(0, 150);
            }

            registrarIngresoInventarioDesdeRecepcion(
                    productoId,
                    idSede,
                    unidadId,
                    cantidad,
                    documentoReferencia,
                    idUsuario,
                    costoUnitarioInventario
            );

            recibioAlgo = true;
        }

        if (!recibioAlgo) {
            throw new RuntimeException("Debe ingresar al menos una cantidad recibida mayor a 0.");
        }

        boolean completa = recepcionDao.compraEstaCompleta(idCompra);

        if (completa) {
            recepcionDao.actualizarEstadoCompra(idCompra, "RECIBIDA_COMPLETA");
        } else {
            recepcionDao.actualizarEstadoCompra(idCompra, "RECIBIDA_PARCIAL");
        }
    }

    @Transactional
    public void cerrarFaltante(
            Integer idCompra,
            Integer idUsuario,
            String motivo,
            String tipoSustento,
            String serieDocumento,
            String numeroDocumento,
            LocalDate fechaDocumento,
            BigDecimal montoAjustado,
            String estadoDevolucion,
            LocalDate fechaDevolucion,
            String observacion
    ) {
        if (idCompra == null || idCompra <= 0) {
            throw new RuntimeException("Compra inválida.");
        }

        Compra compra = compraService.buscarPorId(idCompra);

        if (compra == null) {
            throw new RuntimeException("No se encontró la compra.");
        }

        if (!"RECIBIDA_PARCIAL".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("Solo se puede cerrar faltante en compras recibidas parcialmente.");
        }

        if (idUsuario == null || idUsuario <= 0) {
            throw new RuntimeException("No se encontró el usuario responsable.");
        }

        List<CompraRecepcionDetalle> pendientes = recepcionDao.listarPendientesPorCompra(idCompra);

        if (pendientes == null || pendientes.isEmpty()) {
            throw new RuntimeException("La compra no tiene cantidades pendientes por cerrar.");
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("Debe ingresar el motivo del cierre.");
        }

        if (tipoSustento == null || tipoSustento.trim().isEmpty()) {
            throw new RuntimeException("Debe seleccionar el tipo de sustento.");
        }

        if (estadoDevolucion == null || estadoDevolucion.trim().isEmpty()) {
            throw new RuntimeException("Debe seleccionar el estado de devolución o ajuste.");
        }

        String motivoFinal = motivo.trim().toUpperCase();
        String sustentoFinal = tipoSustento.trim().toUpperCase();
        String estadoDevFinal = estadoDevolucion.trim().toUpperCase();

        if (!estadoDevFinal.equals("NO_APLICA")
                && !estadoDevFinal.equals("PENDIENTE_DEVOLUCION")
                && !estadoDevFinal.equals("DEVUELTO_AJUSTADO")) {
            throw new RuntimeException("Estado de devolución inválido.");
        }

        BigDecimal montoFinal = montoAjustado != null
                ? montoAjustado.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (montoFinal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El monto ajustado no puede ser negativo.");
        }

        String observacionFinal = limpiarTextoOpcional(observacion);

        if (fechaDocumento != null && fechaDocumento.isAfter(LocalDate.now())) {
            throw new RuntimeException("La fecha del documento no puede ser futura.");
        }

        if (fechaDevolucion != null && fechaDevolucion.isAfter(LocalDate.now())) {
            throw new RuntimeException("La fecha de devolución o ajuste no puede ser futura.");
        }

        String serieLimpia = limpiarTextoOpcional(serieDocumento);
        String numeroLimpio = limpiarTextoOpcional(numeroDocumento);

        if (sustentoFinal.equals("NOTA_CREDITO")) {
            if (serieLimpia == null) {
                throw new RuntimeException("La serie de la nota de crédito es obligatoria.");
            }

            if (!serieLimpia.matches("^(FC|BC)\\d{2}$")) {
                throw new RuntimeException("La serie de nota de crédito debe tener formato FC01 o BC01.");
            }

            if (numeroLimpio == null) {
                throw new RuntimeException("El número de la nota de crédito es obligatorio.");
            }

            if (!numeroLimpio.matches("^\\d{1,8}$")) {
                throw new RuntimeException("El número de documento debe contener solo números, máximo 8 dígitos.");
            }

            if (fechaDocumento == null) {
                throw new RuntimeException("La fecha de la nota de crédito es obligatoria.");
            }

            if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El monto ajustado debe ser mayor a 0 para nota de crédito.");
            }
        }

        if (sustentoFinal.equals("DEVOLUCION_BANCARIA")) {
            if (fechaDocumento == null) {
                throw new RuntimeException("La fecha de devolución bancaria es obligatoria.");
            }

            if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El monto devuelto debe ser mayor a 0.");
            }

            if (observacionFinal == null) {
                throw new RuntimeException("Debe ingresar una observación para la devolución bancaria.");
            }
        }

        if (sustentoFinal.equals("ACUERDO_INTERNO") || sustentoFinal.equals("SIN_DOCUMENTO")) {
            if (observacionFinal == null) {
                throw new RuntimeException("Debe ingresar una observación que justifique el cierre.");
            }
        }

        if (observacionFinal != null) {
            if (observacionFinal.length() < 10 || observacionFinal.length() > 500) {
                throw new RuntimeException("La observación debe tener entre 10 y 500 caracteres.");
            }

            if (!observacionFinal.matches("^[A-ZÁÉÍÓÚÑ0-9\\s.,;:()\\-\\/]+$")) {
                throw new RuntimeException("La observación contiene caracteres no permitidos.");
            }
        }

        if (estadoDevFinal.equals("DEVUELTO_AJUSTADO")) {
            if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Si ya fue devuelto o ajustado, el monto ajustado debe ser mayor a 0.");
            }

            if (fechaDevolucion == null) {
                throw new RuntimeException("Debe ingresar la fecha de devolución o ajuste.");
            }
        }

        if (estadoDevFinal.equals("PENDIENTE_DEVOLUCION")) {
            if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Si la devolución está pendiente, el monto ajustado debe ser mayor a 0.");
            }
        }

        if (estadoDevFinal.equals("NO_APLICA")) {
            if (montoFinal.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Si no aplica devolución, el monto ajustado debe ser 0.");
            }

            if (observacionFinal == null) {
                throw new RuntimeException("Debe justificar por qué no aplica devolución o ajuste.");
            }
        }

        BigDecimal valorFaltante = calcularValorFaltante(idCompra, pendientes)
                .setScale(2, RoundingMode.HALF_UP);

        if (valorFaltante.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No se pudo calcular el valor económico del faltante.");
        }

        if (estadoDevFinal.equals("NO_APLICA")) {
            montoFinal = BigDecimal.ZERO;
        }

        if (estadoDevFinal.equals("PENDIENTE_DEVOLUCION")
                || estadoDevFinal.equals("DEVUELTO_AJUSTADO")) {

            if (montoFinal.compareTo(valorFaltante) != 0) {
                throw new RuntimeException(
                        "El monto ajustado debe ser exactamente igual al valor del faltante: S/ "
                                + valorFaltante
                );
            }
        }

        BigDecimal deudaAntes = compra.getDeuda() != null
                ? compra.getDeuda().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal deudaDespues = deudaAntes.subtract(montoFinal);

        BigDecimal saldoFavor = BigDecimal.ZERO;

        if (deudaDespues.compareTo(BigDecimal.ZERO) < 0) {
            saldoFavor = deudaDespues.abs().setScale(2, RoundingMode.HALF_UP);
            deudaDespues = BigDecimal.ZERO;
        }

        BigDecimal montoDevolucionPendiente = BigDecimal.ZERO;

        if (estadoDevFinal.equals("PENDIENTE_DEVOLUCION")) {
            montoDevolucionPendiente = saldoFavor;
        }

        recepcionDao.registrarCierreFaltante(
                idCompra,
                idUsuario,
                motivoFinal,
                sustentoFinal,
                serieLimpia,
                numeroLimpio,
                fechaDocumento,
                montoFinal,
                valorFaltante,
                deudaAntes,
                deudaDespues,
                saldoFavor,
                estadoDevFinal,
                montoDevolucionPendiente,
                fechaDevolucion,
                observacionFinal
        );

        boolean deudaActualizada = compraService.actualizarDeuda(idCompra, deudaDespues);

        if (!deudaActualizada) {
            throw new RuntimeException("No se pudo actualizar la deuda de la compra.");
        }

        recepcionDao.actualizarEstadoCompra(idCompra, "CERRADA_CON_DIFERENCIA");
    }

    private BigDecimal calcularValorFaltante(
            Integer idCompra,
            List<CompraRecepcionDetalle> pendientes
    ) {
        List<DetalleCompra> detallesCompra = compraService.listarDetallesPorCompra(idCompra);

        BigDecimal totalFaltante = BigDecimal.ZERO;

        for (CompraRecepcionDetalle pendiente : pendientes) {
            DetalleCompra original = buscarDetalleCompra(
                    detallesCompra,
                    pendiente.getIdDetalleCompra()
            );

            if (original == null) {
                throw new RuntimeException("No se encontró el detalle original para calcular el faltante.");
            }

            BigDecimal cantidadPendiente = pendiente.getCantidadPendiente() != null
                    ? pendiente.getCantidadPendiente()
                    : BigDecimal.ZERO;

            BigDecimal cantidadOriginal = original.getCantidad() != null
                    ? original.getCantidad()
                    : BigDecimal.ZERO;

            BigDecimal precioUnitario = original.getPrecioUnitario() != null
                    ? original.getPrecioUnitario()
                    : BigDecimal.ZERO;

            BigDecimal descuentoOriginal = original.getDescuento() != null
                    ? original.getDescuento()
                    : BigDecimal.ZERO;

            if (cantidadPendiente.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (cantidadOriginal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("La cantidad original de compra no es válida.");
            }

            BigDecimal descuentoProporcional = BigDecimal.ZERO;

            if (descuentoOriginal.compareTo(BigDecimal.ZERO) > 0) {
                descuentoProporcional = descuentoOriginal
                        .multiply(cantidadPendiente)
                        .divide(cantidadOriginal, 6, RoundingMode.HALF_UP);
            }

            BigDecimal valorLineaFaltante = precioUnitario
                    .multiply(cantidadPendiente)
                    .subtract(descuentoProporcional);

            if (valorLineaFaltante.compareTo(BigDecimal.ZERO) < 0) {
                valorLineaFaltante = BigDecimal.ZERO;
            }

            totalFaltante = totalFaltante.add(valorLineaFaltante);
        }

        return totalFaltante.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void confirmarDevolucionProveedor(
            Integer idCompra,
            LocalDate fechaDevolucion,
            String observacionDevolucion
    ) {
        if (idCompra == null || idCompra <= 0) {
            throw new RuntimeException("Compra inválida.");
        }

        Compra compra = compraService.buscarPorId(idCompra);

        if (compra == null) {
            throw new RuntimeException("No se encontró la compra.");
        }

        if (!"CERRADA_CON_DIFERENCIA".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("Solo se puede confirmar devolución en compras cerradas con diferencia.");
        }

        if (fechaDevolucion == null) {
            fechaDevolucion = LocalDate.now();
        }

        if (fechaDevolucion.isAfter(LocalDate.now())) {
            throw new RuntimeException("La fecha de devolución no puede ser futura.");
        }

        String observacionFinal = limpiarTextoOpcional(observacionDevolucion);

        if (observacionFinal != null) {
            if (observacionFinal.length() < 10 || observacionFinal.length() > 500) {
                throw new RuntimeException("La observación debe tener entre 10 y 500 caracteres.");
            }

            if (!observacionFinal.matches("^[A-ZÁÉÍÓÚÑ0-9\\s.,;:()\\-\\/]+$")) {
                throw new RuntimeException("La observación contiene caracteres no permitidos.");
            }
        }

        boolean actualizado = recepcionDao.confirmarDevolucionProveedor(
                idCompra,
                fechaDevolucion,
                observacionFinal
        );

        if (!actualizado) {
            throw new RuntimeException("No se encontró una devolución pendiente para esta compra.");
        }
    }

    private BigDecimal parseCantidad(List<String> cantidades, int index) {
        try {
            if (cantidades == null || cantidades.size() <= index) {
                return BigDecimal.ZERO;
            }

            String valor = cantidades.get(index);

            if (valor == null || valor.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(valor.trim());

        } catch (Exception e) {
            throw new RuntimeException("Hay cantidades recibidas inválidas.");
        }
    }

    private CompraRecepcionDetalle buscarPendiente(
            List<CompraRecepcionDetalle> pendientes,
            Integer idDetalleCompra
    ) {
        if (pendientes == null || idDetalleCompra == null) {
            return null;
        }

        return pendientes.stream()
                .filter(p -> idDetalleCompra.equals(p.getIdDetalleCompra()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void registrarRecepcionInmediata(
            Integer idCompra,
            Integer idUsuario,
            String observacion
    ) {
        if (idCompra == null || idCompra <= 0) {
            throw new RuntimeException("Compra inválida para recepción inmediata.");
        }

        Compra compra = compraService.buscarPorId(idCompra);

        if (compra == null) {
            throw new RuntimeException("No se encontró la compra para recepción inmediata.");
        }

        if ("BORRADOR".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("No se puede recepcionar una proforma.");
        }

        if ("ANULADA".equalsIgnoreCase(compra.getEstado())) {
            throw new RuntimeException("No se puede recepcionar una compra anulada.");
        }

        if (compra.getIdSede() == null || compra.getIdSede() <= 0) {
            throw new RuntimeException("La compra no tiene sede válida.");
        }

        if (idUsuario == null || idUsuario <= 0) {
            idUsuario = compra.getIdUsuario();
        }

        String nroReferencia = "RI-COMPRA-" + idCompra;

        if (recepcionDao.existeGuiaEnCompra(idCompra, nroReferencia)) {
            throw new RuntimeException("La recepción inmediata de esta compra ya fue registrada.");
        }

        List<CompraRecepcionDetalle> pendientes = recepcionDao.listarPendientesPorCompra(idCompra);
        List<DetalleCompra> detallesCompra = compraService.listarDetallesPorCompra(idCompra);

        if (pendientes == null || pendientes.isEmpty()) {
            throw new RuntimeException("Esta compra no tiene productos pendientes por recibir.");
        }

        CompraRecepcion recepcion = new CompraRecepcion();
        recepcion.setIdCompra(idCompra);
        recepcion.setNroGuiaRemision(nroReferencia);
        recepcion.setFechaRecepcion(LocalDate.now());
        recepcion.setIdSede(compra.getIdSede());
        recepcion.setIdUsuario(idUsuario);
        recepcion.setEstado("REGISTRADA");
        recepcion.setObservacion(
                observacion != null && !observacion.isBlank()
                        ? observacion
                        : "Recepción inmediata completa al registrar la compra."
        );

        int idRecepcion = recepcionDao.registrarRecepcion(recepcion);

        for (CompraRecepcionDetalle p : pendientes) {
            CompraRecepcionDetalle detalle = new CompraRecepcionDetalle();
            detalle.setIdRecepcion(idRecepcion);
            detalle.setIdDetalleCompra(p.getIdDetalleCompra());
            detalle.setIdProducto(p.getIdProducto());
            detalle.setIdUnidad(p.getIdUnidad());
            detalle.setCantidadRecibida(p.getCantidadPendiente());

            recepcionDao.registrarDetalle(detalle);

// Actualiza costo promedio recién cuando ingresa la mercadería.
            DetalleCompra detalleCosto = construirDetalleCosto(
                    detallesCompra,
                    p.getIdDetalleCompra(),
                    p.getCantidadPendiente()
            );

            BigDecimal costoUnitarioInventario =
                    compraService.actualizarCostoPromedioDesdeCompra(compra, detalleCosto);

            String documentoReferencia = "RI-C" + idCompra
                    + " | "
                    + compra.getTipoComprobante()
                    + " "
                    + compra.getSerie()
                    + "-"
                    + compra.getNumero();

            if (documentoReferencia.length() > 150) {
                documentoReferencia = documentoReferencia.substring(0, 150);
            }

            registrarIngresoInventarioDesdeRecepcion(
                    p.getIdProducto(),
                    compra.getIdSede(),
                    p.getIdUnidad(),
                    p.getCantidadPendiente(),
                    documentoReferencia,
                    idUsuario,
                    costoUnitarioInventario
            );
        }

        recepcionDao.actualizarEstadoCompra(idCompra, "RECIBIDA_COMPLETA");
    }
    private DetalleCompra construirDetalleCosto(
            List<DetalleCompra> detallesCompra,
            Integer idDetalleCompra,
            BigDecimal cantidadRecibida
    ) {
        DetalleCompra original = buscarDetalleCompra(detallesCompra, idDetalleCompra);

        if (original == null) {
            throw new RuntimeException("No se encontró el detalle original de la compra para calcular costo promedio.");
        }

        DetalleCompra d = new DetalleCompra();

        d.setIdProducto(original.getIdProducto());
        d.setIdUnidad(original.getIdUnidad());
        d.setCantidad(cantidadRecibida);

        d.setPrecioUnitario(
                original.getPrecioUnitario() != null
                        ? original.getPrecioUnitario()
                        : BigDecimal.ZERO
        );

        BigDecimal descuentoOriginal = original.getDescuento() != null
                ? original.getDescuento()
                : BigDecimal.ZERO;

        BigDecimal cantidadOriginal = original.getCantidad() != null
                ? original.getCantidad()
                : BigDecimal.ZERO;

        BigDecimal descuentoProporcional = BigDecimal.ZERO;

        if (cantidadOriginal.compareTo(BigDecimal.ZERO) > 0
                && descuentoOriginal.compareTo(BigDecimal.ZERO) > 0) {
            descuentoProporcional = descuentoOriginal
                    .multiply(cantidadRecibida)
                    .divide(cantidadOriginal, 6, RoundingMode.HALF_UP);
        }

        d.setDescuento(descuentoProporcional);

        return d;
    }

    private DetalleCompra buscarDetalleCompra(
            List<DetalleCompra> detallesCompra,
            Integer idDetalleCompra
    ) {
        if (detallesCompra == null || idDetalleCompra == null) {
            return null;
        }

        return detallesCompra.stream()
                .filter(d -> idDetalleCompra.equals(d.getIdDetalleCompra()))
                .findFirst()
                .orElse(null);
    }

    private void registrarIngresoInventarioDesdeRecepcion(Integer idProducto,
                                                          Integer idSede,
                                                          Integer idUnidad,
                                                          BigDecimal cantidad,
                                                          String documentoReferencia,
                                                          Integer idUsuario,
                                                          BigDecimal costoUnitarioInventario) {

        Producto producto = productoService.obtener(idProducto);

        if (producto == null) {
            throw new RuntimeException("No se encontró el producto para registrar el ingreso al inventario.");
        }

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La cantidad recibida debe ser mayor a 0.");
        }

        String tipoMedidaBase = producto.getTipoMedidaBase() != null
                ? producto.getTipoMedidaBase().trim().toUpperCase()
                : "";

        boolean ventaPorMedida = Boolean.TRUE.equals(producto.getVentaPorMedida());

        /*
         * 1. PRODUCTOS POR ÁREA
         * Ejemplo: vidrio, parte, lámina.
         * Se registran como partes físicas en inventario_partes.
         */
        boolean esParte = idUnidad != null && idUnidad.equals(ID_UNIDAD_PLANCHA);

        if (!esParte) {
            if (producto.getIdUnidad() != null && producto.getIdUnidad().equals(ID_UNIDAD_PLANCHA)) {
                esParte = true;
            }

            if (producto.getNombreUnidadInventario() != null
                    && producto.getNombreUnidadInventario().trim().equalsIgnoreCase("Parte")) {
                esParte = true;
            }
        }

        if ((ventaPorMedida && "AREA".equals(tipoMedidaBase)) || esParte) {

            Integer medidaVendibleBase = producto.getMedidaVendibleBase();

            if (medidaVendibleBase == null || medidaVendibleBase <= 0) {
                throw new RuntimeException(
                        "El producto " + producto.getNombreProducto()
                                + " no tiene medida vendible configurada. "
                                + "Edita el producto y registra largo/ancho antes de recepcionar la compra."
                );
            }

            BigDecimal cantidadSinDecimales = cantidad.stripTrailingZeros();

            if (cantidadSinDecimales.scale() > 0) {
                throw new RuntimeException("Las partes completas deben recibirse en cantidad entera. Ejemplo: 1, 2, 3.");
            }

            Integer cantidadPartes = cantidad.intValueExact();

            inventarioService.registrarIngresoAreaPorPartes(
                    idProducto,
                    idSede,
                    cantidadPartes,
                    costoUnitarioInventario,
                    medidaVendibleBase,
                    documentoReferencia,
                    idUsuario
            );

            return;
        }

        /*
         * 2. PRODUCTOS POR LONGITUD
         * Ejemplo: aluminio, varilla, perfil, tubo, rollo.
         * También se registran en inventario_partes para saber físicamente:
         * - cuántas varillas completas hay
         * - cuántos cortes quedan
         * - cuántos metros disponibles existen
         */
        if (ventaPorMedida && "LONGITUD".equals(tipoMedidaBase)) {

            Integer medidaVendibleBase = producto.getMedidaVendibleBase();

            if (medidaVendibleBase == null || medidaVendibleBase <= 0) {
                throw new RuntimeException(
                        "El producto " + producto.getNombreProducto()
                                + " no tiene metros vendibles configurados. "
                                + "Edita el producto y registra la medida vendible antes de recepcionar la compra."
                );
            }

            BigDecimal cantidadSinDecimales = cantidad.stripTrailingZeros();

            if (cantidadSinDecimales.scale() > 0) {
                throw new RuntimeException(
                        "Los productos por longitud deben recibirse en unidades completas. Ejemplo: 1, 2, 20 varillas."
                );
            }

            Integer cantidadUnidades = cantidad.intValueExact();

            inventarioService.registrarIngresoLongitudPorPartes(
                    idProducto,
                    idSede,
                    cantidadUnidades,
                    costoUnitarioInventario,
                    BigDecimal.valueOf(medidaVendibleBase),
                    documentoReferencia,
                    idUsuario
            );

            return;
        }

        /*
         * 3. PRODUCTOS NORMALES
         * Ejemplo: silicona, accesorios, tornillos, unidades simples.
         */
        inventarioService.aumentarStock(
                idProducto,
                idSede,
                cantidad,
                documentoReferencia,
                idUsuario,
                costoUnitarioInventario
        );
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }

        return valor.trim().toUpperCase();
    }
}
