package com.vialsa.almacen.model;

import jakarta.validation.constraints.*;

public class Usuario {

    private Integer idUsuario;

    // ======================
    // USUARIO
    // ======================
    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 30, message = "El usuario debe tener entre 4 y 30 caracteres")
    private String nombreUsuario;

    // ======================
    // DATOS PERSONALES
    // ======================
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 50, message = "Los nombres deben tener entre 2 y 50 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 50, message = "Los apellidos deben tener entre 2 y 50 caracteres")
    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe ser un número de 8 dígitos")
    private String nroDocumento;

    @Email(message = "Formato de correo inválido")
    @Size(max = 80, message = "El correo no puede superar 80 caracteres")
    private String correo;

    @Pattern(
            regexp = "^$|^\\d{9}$",
            message = "El teléfono debe tener 9 dígitos"
    )
    private String telefono;


    // ======================
    // DIRECCIÓN
    // ======================
    @Size(max = 120, message = "La dirección no puede superar 120 caracteres")
    private String direccion;

    // ======================
    // CONTRASEÑA
    // ======================
    @Size(min = 8, max = 80, message = "La contraseña debe tener entre 8 y 80 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,}$",
            message = "La contraseña debe tener mayúscula, minúscula, número y símbolo"
    )
    private String contrasena;

    // ======================
    // ROL
    // ======================
    @NotNull(message = "Debe seleccionar un rol")
    private Integer idRol;

    private String nombreRol;

    // ======================
    // ESTADO
    // ======================
    @NotNull(message = "Debe asignar un estado")
    private Boolean activo;

    // FOTO
    private String foto;

    // ======================
    // TIPO DOCUMENTO
    // ======================
    private Integer idTipoDocumento;


    // ======================
    // GETTERS / SETTERS
    // ======================

    public Integer getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
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

    public String getNroDocumento() {
        return nroDocumento;
    }
    public void setNroDocumento(String nroDocumento) {
        this.nroDocumento = nroDocumento;
    }

    public String getCorreo() {
        return correo;
    }
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getContrasena() {
        return contrasena;
    }
    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Integer getIdRol() {
        return idRol;
    }
    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }
    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public Boolean getActivo() {
        return activo;
    }
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getFoto() {
        return foto;
    }
    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Integer getIdTipoDocumento() {
        return idTipoDocumento;
    }
    public void setIdTipoDocumento(Integer idTipoDocumento) {
        this.idTipoDocumento = idTipoDocumento;
    }
}
