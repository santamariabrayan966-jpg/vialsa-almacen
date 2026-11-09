package com.dentia.model.interno;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venta {

    private Integer idVentas;  // ✔ ahora acepta null

    private LocalDateTime fechaVenta;

    // Datos del comprobante
    private String tipoComprobante;
    private String nroComprobante;

    // Relaciones por ID
    private Integer idCliente;
    private Integer idUsuario;
    private Integer idSede;
    private Integer idOrdenOnline;

    // Información de sede
    private String nombreSede;

    // Estados
    private String estadoVenta;

    // Forma de pago
    private String formaPago;

    // Crédito
    private BigDecimal deuda;
    private BigDecimal pagoInicial;
    private int numeroCuotas;

    private String frecuenciaCuotas;
    private java.time.LocalDate fechaPrimeraCuota;

    // Totales
    private BigDecimal totalVenta;

    private String canalVenta;
    private String estadoPago;
    private String codigoTicket;
    private String codigoRecojo;
    private LocalDateTime fechaRecojo;
    private Integer idPedido;
    private Integer idEstadoPedido;
    private String nombreEstadoPedido;
    private LocalDateTime fechaRecojoProgramada;
    private LocalDateTime fechaPreparando;
    private LocalDateTime fechaListo;
    private LocalDateTime fechaEntregado;

    private Integer idUsuarioPreparando;
    private Integer idUsuarioEntrega;

    private String nombreUsuarioPreparando;
    private String nombreUsuarioEntrega;

    private String observacionEntrega;
    private String observacionRecojo;
    private String idTransaccionPasarela;

    // Información del cliente (cuando se registra manual)
    private String documentoCliente;
    private String nombreCliente;
    private String telefonoCliente;
    private String clienteNombres;
    private String clienteApellidos;
    private String clienteRazonSocial;
    private String clienteCorreo;
    private String clienteDireccion;
    private String clienteOrigen;

    // Información adicional
    private String nombreUsuario;

    // Lista de detalles
    private List<DetalleVenta> detalles = new ArrayList<>();

    private String metodoPago;

    private Long minutosAlistado;
    private Long minutosTotalProceso;
    private String tipoEntregaHistorial;

    // Permiso calculado para la acción de anulación en el listado.
    private boolean puedeAnular;
    private String motivoNoAnulacion;


    public boolean isPuedeAnular() { return puedeAnular; }
    public void setPuedeAnular(boolean puedeAnular) { this.puedeAnular = puedeAnular; }

    public String getMotivoNoAnulacion() { return motivoNoAnulacion; }
    public void setMotivoNoAnulacion(String motivoNoAnulacion) { this.motivoNoAnulacion = motivoNoAnulacion; }


    // ===========================
    // GETTERS & SETTERS
    // ===========================

    public Integer getIdVentas() {
        return idVentas;
    }

    public void setIdVentas(Integer idVentas) {
        this.idVentas = idVentas;
    }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

    public Integer getIdUsuario() { return idUsuario;}
    public void setIdUsuario(Integer idUsuario) {this.idUsuario = idUsuario;}

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public Integer getIdOrdenOnline() { return idOrdenOnline; }
    public void setIdOrdenOnline(Integer idOrdenOnline) { this.idOrdenOnline = idOrdenOnline; }

    public String getNombreSede() {
        return nombreSede;
    }

    public void setNombreSede(String nombreSede) {
        this.nombreSede = nombreSede;
    }

    public String getEstadoVenta() { return estadoVenta; }
    public void setEstadoVenta(String estadoVenta) { this.estadoVenta = estadoVenta; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public BigDecimal getDeuda() { return deuda; }
    public void setDeuda(BigDecimal deuda) { this.deuda = deuda; }

    public BigDecimal getPagoInicial() { return pagoInicial; }
    public void setPagoInicial(BigDecimal pagoInicial) { this.pagoInicial = pagoInicial; }

    public int getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(int numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public String getFrecuenciaCuotas() {
        return frecuenciaCuotas;
    }

    public void setFrecuenciaCuotas(String frecuenciaCuotas) {
        this.frecuenciaCuotas = frecuenciaCuotas;
    }

    public java.time.LocalDate getFechaPrimeraCuota() {
        return fechaPrimeraCuota;
    }

    public void setFechaPrimeraCuota(java.time.LocalDate fechaPrimeraCuota) {
        this.fechaPrimeraCuota = fechaPrimeraCuota;
    }

    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }

    public String getCanalVenta() { return canalVenta; }
    public void setCanalVenta(String canalVenta) { this.canalVenta = canalVenta; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getCodigoTicket() { return codigoTicket; }
    public void setCodigoTicket(String codigoTicket) { this.codigoTicket = codigoTicket; }

    public String getCodigoRecojo() { return codigoRecojo; }

    public void setCodigoRecojo(String codigoRecojo) { this.codigoRecojo = codigoRecojo; }

    public LocalDateTime getFechaRecojo() { return fechaRecojo; }
    public void setFechaRecojo(LocalDateTime fechaRecojo) { this.fechaRecojo = fechaRecojo; }

    public String getObservacionRecojo() { return observacionRecojo; }
    public void setObservacionRecojo(String observacionRecojo) { this.observacionRecojo = observacionRecojo; }

    public String getIdTransaccionPasarela() { return idTransaccionPasarela; }
    public void setIdTransaccionPasarela(String idTransaccionPasarela) { this.idTransaccionPasarela = idTransaccionPasarela; }

    public String getDocumentoCliente() { return documentoCliente; }
    public void setDocumentoCliente(String documentoCliente) { this.documentoCliente = documentoCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }

    public String getClienteNombres() { return clienteNombres; }
    public void setClienteNombres(String clienteNombres) { this.clienteNombres = clienteNombres; }

    public String getClienteApellidos() { return clienteApellidos; }
    public void setClienteApellidos(String clienteApellidos) { this.clienteApellidos = clienteApellidos; }

    public String getClienteRazonSocial() { return clienteRazonSocial; }
    public void setClienteRazonSocial(String clienteRazonSocial) { this.clienteRazonSocial = clienteRazonSocial; }

    public String getClienteCorreo() { return clienteCorreo; }
    public void setClienteCorreo(String clienteCorreo) { this.clienteCorreo = clienteCorreo; }

    public String getClienteDireccion() { return clienteDireccion; }
    public void setClienteDireccion(String clienteDireccion) { this.clienteDireccion = clienteDireccion; }

    public String getClienteOrigen() { return clienteOrigen; }
    public void setClienteOrigen(String clienteOrigen) { this.clienteOrigen = clienteOrigen; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public Integer getIdEstadoPedido() {
        return idEstadoPedido;
    }

    public void setIdEstadoPedido(Integer idEstadoPedido) {
        this.idEstadoPedido = idEstadoPedido;
    }

    public String getNombreEstadoPedido() {
        return nombreEstadoPedido;
    }

    public void setNombreEstadoPedido(String nombreEstadoPedido) {
        this.nombreEstadoPedido = nombreEstadoPedido;
    }

    public LocalDateTime getFechaRecojoProgramada() {
        return fechaRecojoProgramada;
    }

    public void setFechaRecojoProgramada(LocalDateTime fechaRecojoProgramada) {
        this.fechaRecojoProgramada = fechaRecojoProgramada;
    }
    public LocalDateTime getFechaPreparando() {
        return fechaPreparando;
    }

    public void setFechaPreparando(LocalDateTime fechaPreparando) {
        this.fechaPreparando = fechaPreparando;
    }

    public LocalDateTime getFechaListo() {
        return fechaListo;
    }

    public void setFechaListo(LocalDateTime fechaListo) {
        this.fechaListo = fechaListo;
    }

    public LocalDateTime getFechaEntregado() {
        return fechaEntregado;
    }

    public void setFechaEntregado(LocalDateTime fechaEntregado) {
        this.fechaEntregado = fechaEntregado;
    }

    public Integer getIdUsuarioPreparando() {
        return idUsuarioPreparando;
    }

    public void setIdUsuarioPreparando(Integer idUsuarioPreparando) {
        this.idUsuarioPreparando = idUsuarioPreparando;
    }

    public Integer getIdUsuarioEntrega() {
        return idUsuarioEntrega;
    }

    public void setIdUsuarioEntrega(Integer idUsuarioEntrega) {
        this.idUsuarioEntrega = idUsuarioEntrega;
    }

    public String getNombreUsuarioPreparando() {
        return nombreUsuarioPreparando;
    }

    public void setNombreUsuarioPreparando(String nombreUsuarioPreparando) {
        this.nombreUsuarioPreparando = nombreUsuarioPreparando;
    }

    public String getNombreUsuarioEntrega() {
        return nombreUsuarioEntrega;
    }

    public void setNombreUsuarioEntrega(String nombreUsuarioEntrega) {
        this.nombreUsuarioEntrega = nombreUsuarioEntrega;
    }

    public String getObservacionEntrega() {
        return observacionEntrega;
    }

    public void setObservacionEntrega(String observacionEntrega) {
        this.observacionEntrega = observacionEntrega;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Long getMinutosAlistado() {
        return minutosAlistado;
    }

    public void setMinutosAlistado(Long minutosAlistado) {
        this.minutosAlistado = minutosAlistado;
    }

    public Long getMinutosTotalProceso() {
        return minutosTotalProceso;
    }

    public void setMinutosTotalProceso(Long minutosTotalProceso) {
        this.minutosTotalProceso = minutosTotalProceso;
    }
    public String getTipoEntregaHistorial() {
        return tipoEntregaHistorial;
    }

    public void setTipoEntregaHistorial(String tipoEntregaHistorial) {
        this.tipoEntregaHistorial = tipoEntregaHistorial;
    }
}