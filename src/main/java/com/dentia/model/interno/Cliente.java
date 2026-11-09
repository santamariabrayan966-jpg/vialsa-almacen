package com.dentia.model.interno;

import java.sql.Timestamp;

public class Cliente {

    private Integer idClientes;
    private String nombres;
    private String apellidos;
    private String razonSocial;
    private String nro_documento;
    private String direccion;
    private String telefono;
    private String correo;

    private Integer idTipoDocumento;

    private boolean vip;
    private boolean moroso;
    private boolean activo;
    private String foto;
    private Timestamp fecha_registro;

    private String googleSub;
    private boolean cuentaOnlineActiva;
    private Timestamp fechaVinculacionGoogle;
    private boolean perfilCompleto;

    private boolean sujetoDetraccion;

    private String tipoDocumentoNombre;

    public Integer getIdClientes() {
        return idClientes;
    }

    public void setIdClientes(Integer idClientes) {
        this.idClientes = idClientes;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNro_documento() {
        return nro_documento;
    }

    public void setNro_documento(String nro_documento) {
        this.nro_documento = nro_documento;
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

    public Integer getIdTipoDocumento() {
        return idTipoDocumento;
    }

    public void setIdTipoDocumento(Integer idTipoDocumento) {
        this.idTipoDocumento = idTipoDocumento;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public boolean isMoroso() {
        return moroso;
    }

    public void setMoroso(boolean moroso) {
        this.moroso = moroso;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Timestamp getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(Timestamp fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public Timestamp getFechaRegistro() {
        return fecha_registro;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public boolean isCuentaOnlineActiva() {
        return cuentaOnlineActiva;
    }

    public void setCuentaOnlineActiva(boolean cuentaOnlineActiva) {
        this.cuentaOnlineActiva = cuentaOnlineActiva;
    }

    public Timestamp getFechaVinculacionGoogle() {
        return fechaVinculacionGoogle;
    }

    public void setFechaVinculacionGoogle(Timestamp fechaVinculacionGoogle) {
        this.fechaVinculacionGoogle = fechaVinculacionGoogle;
    }

    public boolean isPerfilCompleto() {
        return perfilCompleto;
    }

    public void setPerfilCompleto(boolean perfilCompleto) {
        this.perfilCompleto = perfilCompleto;
    }

    public boolean isSujetoDetraccion() {
        return sujetoDetraccion;
    }

    public void setSujetoDetraccion(boolean sujetoDetraccion) {
        this.sujetoDetraccion = sujetoDetraccion;
    }

    public String getTipoDocumentoNombre() {
        return tipoDocumentoNombre;
    }

    public void setTipoDocumentoNombre(String tipoDocumentoNombre) {
        this.tipoDocumentoNombre = tipoDocumentoNombre;
    }
}
