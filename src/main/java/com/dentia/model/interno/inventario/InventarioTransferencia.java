package com.dentia.model.interno.inventario;

import java.time.LocalDateTime;
import java.util.List;

public class InventarioTransferencia {

    private Integer idTransferencia;
    private String codigo;
    private Integer idSedeOrigen;
    private Integer idSedeDestino;
    private Integer idUsuario;

    // NUEVO: usuario transportista asignado o que acepta traslado
    private Integer idTransportista;

    private String estado;

    // NUEVO: INMEDIATA / CON_TRANSPORTE
    private String tipoTransferencia;

    // NUEVO: 0 = no, 1 = sí
    private Boolean requiereTransporte;

    private String observacion;
    private LocalDateTime fechaTransferencia;

    // NUEVO: fechas del flujo logístico
    private LocalDateTime fechaAceptacion;
    private LocalDateTime fechaLlegadaOrigen;
    private LocalDateTime fechaPedidoListo;
    private LocalDateTime fechaInicioRuta;
    private LocalDateTime fechaLlegadaDestino;
    private LocalDateTime fechaRecepcion;
    private LocalDateTime fechaFinalizacion;


    // Fotos del traslado
    private String fotoLlegadaOrigen;
    private String fotoCarga;
    private String fotoLlegada;

    // NUEVO: observación final de recepción
    private String observacionRecepcion;

    // Campos auxiliares para mostrar en vistas
    private String nombreSedeOrigen;
    private String nombreSedeDestino;
    private String nombreUsuario;
    private String nombreTransportista;

    private List<InventarioTransferenciaDetalle> detalles;

    public Integer getIdTransferencia() {
        return idTransferencia;
    }

    public void setIdTransferencia(Integer idTransferencia) {
        this.idTransferencia = idTransferencia;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Integer getIdSedeOrigen() {
        return idSedeOrigen;
    }

    public void setIdSedeOrigen(Integer idSedeOrigen) {
        this.idSedeOrigen = idSedeOrigen;
    }

    public Integer getIdSedeDestino() {
        return idSedeDestino;
    }

    public void setIdSedeDestino(Integer idSedeDestino) {
        this.idSedeDestino = idSedeDestino;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdTransportista() {
        return idTransportista;
    }

    public void setIdTransportista(Integer idTransportista) {
        this.idTransportista = idTransportista;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoTransferencia() {
        return tipoTransferencia;
    }

    public void setTipoTransferencia(String tipoTransferencia) {
        this.tipoTransferencia = tipoTransferencia;
    }

    public Boolean getRequiereTransporte() {
        return requiereTransporte;
    }

    public void setRequiereTransporte(Boolean requiereTransporte) {
        this.requiereTransporte = requiereTransporte;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaTransferencia() {
        return fechaTransferencia;
    }

    public void setFechaTransferencia(LocalDateTime fechaTransferencia) {
        this.fechaTransferencia = fechaTransferencia;
    }

    public LocalDateTime getFechaAceptacion() {
        return fechaAceptacion;
    }

    public void setFechaAceptacion(LocalDateTime fechaAceptacion) {
        this.fechaAceptacion = fechaAceptacion;
    }

    public LocalDateTime getFechaLlegadaOrigen() {
        return fechaLlegadaOrigen;
    }

    public void setFechaLlegadaOrigen(LocalDateTime fechaLlegadaOrigen) {
        this.fechaLlegadaOrigen = fechaLlegadaOrigen;
    }

    public LocalDateTime getFechaPedidoListo() {
        return fechaPedidoListo;
    }

    public void setFechaPedidoListo(LocalDateTime fechaPedidoListo) {
        this.fechaPedidoListo = fechaPedidoListo;
    }

    public LocalDateTime getFechaInicioRuta() {
        return fechaInicioRuta;
    }

    public void setFechaInicioRuta(LocalDateTime fechaInicioRuta) {
        this.fechaInicioRuta = fechaInicioRuta;
    }

    public LocalDateTime getFechaLlegadaDestino() {
        return fechaLlegadaDestino;
    }

    public void setFechaLlegadaDestino(LocalDateTime fechaLlegadaDestino) {
        this.fechaLlegadaDestino = fechaLlegadaDestino;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDateTime fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public String getFotoLlegadaOrigen() {
        return fotoLlegadaOrigen;
    }

    public void setFotoLlegadaOrigen(String fotoLlegadaOrigen) {
        this.fotoLlegadaOrigen = fotoLlegadaOrigen;
    }

    public String getFotoCarga() {
        return fotoCarga;
    }

    public void setFotoCarga(String fotoCarga) {
        this.fotoCarga = fotoCarga;
    }

    public String getFotoLlegada() {
        return fotoLlegada;
    }

    public void setFotoLlegada(String fotoLlegada) {
        this.fotoLlegada = fotoLlegada;
    }

    public String getObservacionRecepcion() {
        return observacionRecepcion;
    }

    public void setObservacionRecepcion(String observacionRecepcion) {
        this.observacionRecepcion = observacionRecepcion;
    }

    public String getNombreSedeOrigen() {
        return nombreSedeOrigen;
    }

    public void setNombreSedeOrigen(String nombreSedeOrigen) {
        this.nombreSedeOrigen = nombreSedeOrigen;
    }

    public String getNombreSedeDestino() {
        return nombreSedeDestino;
    }

    public void setNombreSedeDestino(String nombreSedeDestino) {
        this.nombreSedeDestino = nombreSedeDestino;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreTransportista() {
        return nombreTransportista;
    }

    public void setNombreTransportista(String nombreTransportista) {
        this.nombreTransportista = nombreTransportista;
    }

    public List<InventarioTransferenciaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<InventarioTransferenciaDetalle> detalles) {
        this.detalles = detalles;
    }
}
