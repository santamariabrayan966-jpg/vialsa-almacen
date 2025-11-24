package com.vialsa.almacen.controller;

import jakarta.validation.constraints.*;

public class RegistroClienteDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo inválido")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]{5,}@(gmail\\.com|hotmail\\.com|outlook\\.com|yahoo\\.com|icloud\\.com)$",
            message = "Debe usar un correo válido de Gmail, Hotmail, Outlook, Yahoo o iCloud"
    )
    @Size(max = 80, message = "Máximo 80 caracteres")
    private String correo;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,60}$",
            message = "El nombre solo debe contener letras y espacios (2–60 caracteres)"
    )
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{2,80}$",
            message = "Los apellidos solo deben contener letras y espacios (2–80 caracteres)"
    )
    private String apellidos;

    @NotNull(message = "Seleccione un tipo de documento")
    private Integer idTipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String nroDocumento;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(
            regexp = "\\d{9}",
            message = "El celular debe tener 9 dígitos"
    )
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,60}$",
            message = "Debe tener mínimo 8 caracteres, mayúscula, minúscula, número y símbolo"
    )
    private String password;

    // =====================================================
    //  VALIDACIÓN CRUZADA (DNI 8 dígitos, RUC 11 dígitos)
    // =====================================================
    @AssertTrue(message = "DNI debe tener 8 dígitos o RUC debe tener 11 dígitos")
    public boolean isDocumentoValido() {

        if (idTipoDocumento == null || nroDocumento == null)
            return false;

        return switch (idTipoDocumento) {
            case 1 -> nroDocumento.matches("\\d{8}");   // DNI
            case 2 -> nroDocumento.matches("\\d{11}");  // RUC
            default -> true;
        };
    }

    public RegistroClienteDTO() {}

    // =====================================================
    //  GETTERS & SETTERS COMPLETOS
    // =====================================================

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
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

    public Integer getIdTipoDocumento() {
        return idTipoDocumento;
    }

    public void setIdTipoDocumento(Integer idTipoDocumento) {
        this.idTipoDocumento = idTipoDocumento;
    }

    public String getNroDocumento() {
        return nroDocumento;
    }

    public void setNroDocumento(String nroDocumento) {
        this.nroDocumento = nroDocumento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
