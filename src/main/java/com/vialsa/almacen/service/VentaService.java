package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.interfaces.IDetalleVentaDao;
import com.vialsa.almacen.dao.interfaces.IVentaDao;
import com.vialsa.almacen.model.DetalleVenta;
import com.vialsa.almacen.model.Movimiento;
import com.vialsa.almacen.model.Venta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vialsa.almacen.dao.interfaces.IClienteDao;
import com.vialsa.almacen.model.Cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VentaService {

    private final IVentaDao ventaDao;
    private final IDetalleVentaDao detalleDao;
    private final ProductoService productoService;
    private final MovimientoService movimientoService;
    private final IClienteDao clienteDao;

    public VentaService(
            IVentaDao ventaDao,
            IDetalleVentaDao detalleDao,
            ProductoService productoService,
            MovimientoService movimientoService,
            IClienteDao clienteDao
    ) {
        this.ventaDao = ventaDao;
        this.detalleDao = detalleDao;
        this.productoService = productoService;
        this.movimientoService = movimientoService;
        this.clienteDao = clienteDao;
    }

    // ============================================================
    // LISTAR Y CONSULTAR
    // ============================================================
    public List<Venta> listar() {
        return ventaDao.listar();
    }

    public Venta buscarPorId(int idVenta) {
        Venta v = ventaDao.buscarPorId(idVenta);
        if (v != null) {
            v.setDetalles(detalleDao.listarPorVenta(idVenta));
        }
        return v;
    }

    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        return ventaDao.obtenerIdUsuarioPorNombre(nombreUsuario);
    }


    // ============================================================
    // CREAR BORRADOR
    // ============================================================
    @Transactional
    public int crearBorrador(Venta venta) {

        // Registrar cliente manual si no es externo
        if (venta.getIdCliente() == null && venta.getNombreCliente() != null) {
            Integer nuevoClienteId = crearClienteManual(
                    venta.getDocumentoCliente(),
                    venta.getNombreCliente(),
                    venta.getTelefonoCliente()
            );
            venta.setIdCliente(nuevoClienteId);
        }

        venta.setEstadoVenta("BORRADOR");
        venta.setTotalVenta(BigDecimal.ZERO);
        venta.setDeuda(BigDecimal.ZERO);

        return ventaDao.registrarYObtenerId(venta);
    }


    // ============================================================
    // AGREGAR DETALLE
    // ============================================================
    @Transactional
    public void agregarDetalle(int idVenta, DetalleVenta d) {

        BigDecimal stock = productoService.obtenerStockActual(d.getIdProducto());
        if (stock.compareTo(d.getCantidad()) < 0) {
            throw new RuntimeException("Stock insuficiente del producto ID " + d.getIdProducto());
        }

        d.setIdVenta(idVenta);
        detalleDao.registrar(d);
    }


    // ============================================================
    // CONFIRMAR VENTA
    // ============================================================
    @Transactional
    public void confirmarVenta(int idVenta) {

        Venta venta = buscarPorId(idVenta);

        if (venta == null)
            throw new RuntimeException("No existe la venta.");

        if (!"BORRADOR".equals(venta.getEstadoVenta()))
            throw new RuntimeException("Solo se pueden confirmar ventas en BORRADOR.");

        if (venta.getDetalles() == null || venta.getDetalles().isEmpty())
            throw new RuntimeException("La venta no tiene detalles registrados.");

        // Fecha real de la venta
        venta.setFechaVenta(LocalDateTime.now());

        // Calcular total
        BigDecimal total = venta.getDetalles().stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venta.setTotalVenta(total);

        // Tipo de comprobante obligatorio
        if (venta.getTipoComprobante() == null)
            venta.setTipoComprobante("NOTA");

        // Generar número
        String nro = ventaDao.obtenerSiguienteComprobante(venta.getTipoComprobante());
        venta.setNroComprobante(nro);

        // Estado final
        venta.setEstadoVenta(
                "CREDITO".equalsIgnoreCase(venta.getFormaPago()) ? "CREDITO" : "COMPLETADA"
        );

        // Guardar cambios
        ventaDao.actualizarVenta(venta);

        // Registrar movimientos
        for (DetalleVenta d : venta.getDetalles()) {

            productoService.descontarStock(d.getIdProducto(), d.getCantidad());

            Movimiento mov = new Movimiento();
            mov.setTipoMovimiento("SALIDA");
            mov.setCantidad(d.getCantidad());
            mov.setIdProducto(d.getIdProducto());
            mov.setIdUnidad(d.getIdUnidad());
            mov.setIdUsuario(venta.getIdUsuario());
            mov.setFecha(LocalDateTime.now());
            mov.setOrigen("VENTA");
            mov.setIdDocumento(idVenta);
            mov.setObservacion("Salida por venta confirmada");

            movimientoService.registrar(mov);
        }
    }


    // ============================================================
    // ANULAR VENTA
    // ============================================================
    @Transactional
    public void anularVenta(int idVenta) {

        Venta venta = buscarPorId(idVenta);

        if (venta == null)
            throw new RuntimeException("Venta no encontrada.");

        if ("ANULADA".equals(venta.getEstadoVenta()))
            throw new RuntimeException("La venta ya está anulada.");

        if ("BORRADOR".equals(venta.getEstadoVenta()))
            throw new RuntimeException("Una venta BORRADOR no se anula, se elimina.");

        for (DetalleVenta d : venta.getDetalles()) {

            productoService.agregarStock(d.getIdProducto(), d.getCantidad());

            Movimiento mov = new Movimiento();
            mov.setTipoMovimiento("ENTRADA");
            mov.setCantidad(d.getCantidad());
            mov.setIdProducto(d.getIdProducto());
            mov.setIdUnidad(d.getIdUnidad());
            mov.setIdUsuario(venta.getIdUsuario());
            mov.setFecha(LocalDateTime.now());
            mov.setOrigen("DEVOLUCION");
            mov.setIdDocumento(idVenta);
            mov.setObservacion("Entrada por anulación de venta");

            movimientoService.registrar(mov);
        }

        ventaDao.actualizarEstado(idVenta, "ANULADA");
    }


    // ============================================================
    // ELIMINAR BORRADOR
    // ============================================================
    @Transactional
    public void eliminarBorrador(int idVenta) {

        Venta venta = buscarPorId(idVenta);

        if (!"BORRADOR".equals(venta.getEstadoVenta()))
            throw new RuntimeException("Solo se pueden eliminar ventas BORRADOR.");

        detalleDao.eliminarPorVenta(idVenta);
        ventaDao.eliminarVenta(idVenta);
    }


    // ============================================================
    // REGISTRAR CRÉDITO
    // ============================================================
    @Transactional
    public void registrarCredito(int idVenta, BigDecimal pagoInicial, BigDecimal deuda) {

        Venta venta = buscarPorId(idVenta);

        venta.setFormaPago("CREDITO");
        venta.setPagoInicial(pagoInicial);
        venta.setDeuda(deuda);
        venta.setEstadoVenta("CREDITO");

        ventaDao.actualizarVenta(venta);
    }


    // ============================================================
    // CLIENTE MANUAL
    // ============================================================
    public Integer crearClienteManual(String documento, String nombre, String telefono) {

        // 1️⃣ Verificar si ya existe el cliente
        Cliente existente = clienteDao.buscarPorDocumento(documento);
        if (existente != null) {
            System.out.println("✔ Cliente ya existe, usando su ID: " + existente.getIdClientes());
            return existente.getIdClientes();
        }

        // 2️⃣ Registrar un cliente nuevo
        Cliente c = new Cliente();
        c.setNro_documento(documento);
        c.setNombres(nombre);
        c.setApellidos(""); // vacío
        c.setTelefono(telefono);

        Cliente creado = clienteDao.crearAutomatico(c);

        System.out.println("➕ Cliente creado automáticamente con ID: " + creado.getIdClientes());
        return creado.getIdClientes();
    }

    public String obtenerSiguienteComprobante(String tipoComprobante) {
        return ventaDao.obtenerSiguienteComprobante(tipoComprobante);
    }

}
