package com.dentia.dto.paciente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RegistroPacienteDto {

    @NotBlank(message = "Selecciona el tipo de registro.")
    @Pattern(regexp = "CON_DNI|SIN_DNI", message = "El tipo de registro no es valido.")
    private String tipoRegistro = "CON_DNI";

    private String dni;

    @NotBlank(message = "Ingresa tus nombres.")
    @Size(max = 100, message = "Los nombres son demasiado largos.")
    private String nombres;

    @NotBlank(message = "Ingresa tus apellidos.")
    @Size(max = 120, message = "Los apellidos son demasiado largos.")
    private String apellidos;

    @NotNull(message = "Ingresa tu fecha de nacimiento.")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy.")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "Ingresa un numero de celular.")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "El celular debe contener entre 7 y 15 digitos.")
    private String celular;

    @NotBlank(message = "Ingresa tu correo.")
    @Email(message = "Ingresa un correo valido.")
    @Size(max = 191, message = "El correo es demasiado largo.")
    private String correo;

    @NotBlank(message = "Crea una contrasena.")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.")
    private String password;

    @NotBlank(message = "Confirma tu contrasena.")
    private String confirmarPassword;

    public String getTipoRegistro() { return tipoRegistro; }
    public void setTipoRegistro(String tipoRegistro) { this.tipoRegistro = tipoRegistro; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmarPassword() { return confirmarPassword; }
    public void setConfirmarPassword(String confirmarPassword) { this.confirmarPassword = confirmarPassword; }
}
