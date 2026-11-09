package com.dentia.model.interno;

import java.time.LocalDateTime;

public class ProductoFoto {

    private Integer idProductoFoto;
    private Integer idProducto;
    private String nombreArchivo;
    private String urlArchivo;
    private Boolean portada;
    private Integer orden;
    private Integer estado;
    private LocalDateTime fechaCreacion;

    public Integer getIdProductoFoto() {
        return idProductoFoto;
    }

    public void setIdProductoFoto(Integer idProductoFoto) {
        this.idProductoFoto = idProductoFoto;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo;
    }

    public Boolean getPortada() {
        return portada;
    }

    public void setPortada(Boolean portada) {
        this.portada = portada;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
