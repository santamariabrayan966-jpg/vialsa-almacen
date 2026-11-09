package com.dentia.dto;

import java.time.LocalDateTime;

public class CheckoutOnlineDTO {

    private Integer idCliente;
    private LocalDateTime fechaRecojo;
    private String observacionRecojo;
    private String metodoPago;

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public LocalDateTime getFechaRecojo() {
        return fechaRecojo;
    }

    public void setFechaRecojo(LocalDateTime fechaRecojo) {
        this.fechaRecojo = fechaRecojo;
    }

    public String getObservacionRecojo() {
        return observacionRecojo;
    }

    public void setObservacionRecojo(String observacionRecojo) {
        this.observacionRecojo = observacionRecojo;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
}