package com.dentia.model.interno;

import java.time.LocalDateTime;

public class InventarioReserva {

    private Integer idReserva;
    private String codigoReserva;
    private Integer idOrdenOnline;
    private Integer idCliente;
    private Integer idSede;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private LocalDateTime fechaConfirmacion;
    private LocalDateTime fechaLiberacion;
    private String motivoLiberacion;
    private String tokenIdempotencia;

    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }
    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }
    public Integer getIdOrdenOnline() { return idOrdenOnline; }
    public void setIdOrdenOnline(Integer idOrdenOnline) { this.idOrdenOnline = idOrdenOnline; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }
    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }
    public LocalDateTime getFechaLiberacion() { return fechaLiberacion; }
    public void setFechaLiberacion(LocalDateTime fechaLiberacion) { this.fechaLiberacion = fechaLiberacion; }
    public String getMotivoLiberacion() { return motivoLiberacion; }
    public void setMotivoLiberacion(String motivoLiberacion) { this.motivoLiberacion = motivoLiberacion; }
    public String getTokenIdempotencia() { return tokenIdempotencia; }
    public void setTokenIdempotencia(String tokenIdempotencia) { this.tokenIdempotencia = tokenIdempotencia; }
}
