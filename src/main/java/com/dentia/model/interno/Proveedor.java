package com.dentia.model.interno;

import java.time.LocalDateTime;

public class Proveedor {

    private Integer idProveedor;
    private String nombreProveedor;
    private String nroDocumento;
    private String direccion;
    private String telefono;
    private String correo;

    // 1 = activo, 0 = inactivo, 2 = eliminado lógicamente.
    private Integer estado = 1;

    // Fecha de registro (opcional, por si luego la muestras)
    private LocalDateTime fechaRegistro;

    // Getters y setters

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getNroDocumento() {
        return nroDocumento;
    }

    public void setNroDocumento(String nroDocumento) {
        this.nroDocumento = nroDocumento;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public boolean isActivo() {
        return Integer.valueOf(1).equals(estado);
    }

    public void setActivo(boolean activo) {
        this.estado = activo ? 1 : 0;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
