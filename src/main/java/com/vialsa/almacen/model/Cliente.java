package com.vialsa.almacen.model;

import java.sql.Timestamp;

public class Cliente {

    private Integer idClientes;
    private String nombres;
    private String apellidos;
    private String nro_documento;
    private String direccion;
    private String telefono;
    private String correo;

    private Integer idTipoDocumento;
    private Integer idUsuario;

    // ===== NUEVOS CAMPOS PRO =====
    private boolean vip;
    private boolean moroso;
    private boolean activo;
    private String foto;
    private Timestamp fecha_registro;

    // ===== CAMPOS EXTRA PERFIL COMPLETO =====
    private String tipoDocumentoNombre;   // nombre del tipo doc
    private String usuarioRegistro;       // usuario que creó el registro

    // ===== GETTERS & SETTERS =====

    public Integer getIdClientes() { return idClientes; }
    public void setIdClientes(Integer idClientes) { this.idClientes = idClientes; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getNro_documento() { return nro_documento; }
    public void setNro_documento(String nro_documento) { this.nro_documento = nro_documento; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public Integer getIdTipoDocumento() { return idTipoDocumento; }
    public void setIdTipoDocumento(Integer idTipoDocumento) { this.idTipoDocumento = idTipoDocumento; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }

    public boolean isMoroso() { return moroso; }
    public void setMoroso(boolean moroso) { this.moroso = moroso; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public Timestamp getFecha_registro() { return fecha_registro; }
    public void setFecha_registro(Timestamp fecha_registro) { this.fecha_registro = fecha_registro; }

    // Getter estándar para usar en Thymeleaf
    public Timestamp getFechaRegistro() { return fecha_registro; }

    public String getTipoDocumentoNombre() { return tipoDocumentoNombre; }
    public void setTipoDocumentoNombre(String tipoDocumentoNombre) { this.tipoDocumentoNombre = tipoDocumentoNombre; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
