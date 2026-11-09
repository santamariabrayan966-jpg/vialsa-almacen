package com.dentia.model.interno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CompraRecepcion {

    private Integer idRecepcion;
    private Integer idCompra;
    private String nroGuiaRemision;
    private LocalDate fechaRecepcion;
    private Integer idSede;
    private Integer idUsuario;
    private String estado;
    private String observacion;
    private LocalDateTime fechaRegistro;

    private String proveedor;
    private String comprobante;
    private String sedeNombre;
    private List<CompraRecepcionDetalle> detalles;

    public Integer getIdRecepcion() { return idRecepcion; }
    public void setIdRecepcion(Integer idRecepcion) { this.idRecepcion = idRecepcion; }

    public Integer getIdCompra() { return idCompra; }
    public void setIdCompra(Integer idCompra) { this.idCompra = idCompra; }

    public String getNroGuiaRemision() { return nroGuiaRemision; }
    public void setNroGuiaRemision(String nroGuiaRemision) { this.nroGuiaRemision = nroGuiaRemision; }

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getComprobante() { return comprobante; }
    public void setComprobante(String comprobante) { this.comprobante = comprobante; }

    public String getSedeNombre() { return sedeNombre; }
    public void setSedeNombre(String sedeNombre) { this.sedeNombre = sedeNombre; }

    public List<CompraRecepcionDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<CompraRecepcionDetalle> detalles) { this.detalles = detalles; }
}