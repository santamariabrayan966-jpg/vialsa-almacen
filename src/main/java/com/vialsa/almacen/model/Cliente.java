package com.vialsa.almacen.model;

public class Cliente {
    private int idClientes;
    private String nombres;
    private String apellidos;
    private String nro_documento; // ✅ igual que en la base
    private String direccion;
    private String telefono;
    private String correo;

    // Getters y Setters
    public int getIdClientes() { return idClientes; }
    public void setIdClientes(int idClientes) { this.idClientes = idClientes; }

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
}
