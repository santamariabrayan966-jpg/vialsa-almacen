package com.dentia.model.interno;

import java.time.LocalDateTime;

public class ProductoPresentacionSede {

    private Integer idPresentacionSede;
    private Integer idPrecioPresentacion;
    private Integer idSede;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String nombreSede;
    private String tipoSede;
    private String ciudadSede;

    public Integer getIdPresentacionSede() {
        return idPresentacionSede;
    }

    public void setIdPresentacionSede(Integer idPresentacionSede) {
        this.idPresentacionSede = idPresentacionSede;
    }

    public Integer getIdPrecioPresentacion() {
        return idPrecioPresentacion;
    }

    public void setIdPrecioPresentacion(Integer idPrecioPresentacion) {
        this.idPrecioPresentacion = idPrecioPresentacion;
    }

    public Integer getIdSede() {
        return idSede;
    }

    public void setIdSede(Integer idSede) {
        this.idSede = idSede;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getNombreSede() {
        return nombreSede;
    }

    public void setNombreSede(String nombreSede) {
        this.nombreSede = nombreSede;
    }

    public String getTipoSede() {
        return tipoSede;
    }

    public void setTipoSede(String tipoSede) {
        this.tipoSede = tipoSede;
    }

    public String getCiudadSede() {
        return ciudadSede;
    }

    public void setCiudadSede(String ciudadSede) {
        this.ciudadSede = ciudadSede;
    }
}
