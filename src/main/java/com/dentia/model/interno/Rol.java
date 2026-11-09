package com.dentia.model.interno;

public class Rol {
    private Integer idRol;
    private String nombreRol;
    private String descripcion;
    private Boolean activo;
    private Integer cantidadUsuarios;

    public Rol() {}
    public Rol(Integer idRol, String nombreRol) { this.idRol=idRol; this.nombreRol=nombreRol; }
    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol=idRol; }
    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String nombreRol) { this.nombreRol=nombreRol; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion=descripcion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo=activo; }
    public Integer getCantidadUsuarios() { return cantidadUsuarios == null ? 0 : cantidadUsuarios; }
    public void setCantidadUsuarios(Integer cantidadUsuarios) { this.cantidadUsuarios=cantidadUsuarios; }
}
