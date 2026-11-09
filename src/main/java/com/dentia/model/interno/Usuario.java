package com.dentia.model.interno;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Usuario {
    private Integer idUsuario;
    @Size(min=4,max=30) private String nombreUsuario;
    @Size(min=2,max=50) private String nombres;
    @Size(min=2,max=50) private String apellidos;
    @Pattern(regexp="^$|\\d{8}",message="El DNI debe tener 8 digitos") private String nroDocumento;
    @Email @Size(max=180) private String correo;
    private String correoRecuperacion;
    @Size(max=30) private String telefono;
    private String direccion;
    private String contrasena;
    private Integer idRol;
    private String nombreRol;
    private Integer idSedePrincipal;
    private String nombreSedePrincipal;
    private Boolean puedeCambiarSede=false;
    private Integer idEstadoUsuario;
    private String nombreEstado;
    private String foto;
    private Integer idTipoDocumento;

    public Integer getIdUsuario(){return idUsuario;} public void setIdUsuario(Integer v){idUsuario=v;}
    public String getNombreUsuario(){return nombreUsuario;} public void setNombreUsuario(String v){nombreUsuario=v;}
    public String getNombres(){return nombres;} public void setNombres(String v){nombres=v;}
    public String getApellidos(){return apellidos;} public void setApellidos(String v){apellidos=v;}
    public String getNroDocumento(){return nroDocumento;} public void setNroDocumento(String v){nroDocumento=v;}
    public String getCorreo(){return correo;} public void setCorreo(String v){correo=v;}
    public String getCorreoRecuperacion(){return correoRecuperacion;} public void setCorreoRecuperacion(String v){correoRecuperacion=v;}
    public String getTelefono(){return telefono;} public void setTelefono(String v){telefono=v;}
    public String getDireccion(){return direccion;} public void setDireccion(String v){direccion=v;}
    public String getContrasena(){return contrasena;} public void setContrasena(String v){contrasena=v;}
    public Integer getIdRol(){return idRol;} public void setIdRol(Integer v){idRol=v;}
    public String getNombreRol(){return nombreRol;} public void setNombreRol(String v){nombreRol=v;}
    public Integer getIdSedePrincipal(){return idSedePrincipal;} public void setIdSedePrincipal(Integer v){idSedePrincipal=v;}
    public String getNombreSedePrincipal(){return nombreSedePrincipal;} public void setNombreSedePrincipal(String v){nombreSedePrincipal=v;}
    public Boolean getPuedeCambiarSede(){return puedeCambiarSede;} public void setPuedeCambiarSede(Boolean v){puedeCambiarSede=v;}
    public Integer getIdEstadoUsuario(){return idEstadoUsuario;} public void setIdEstadoUsuario(Integer v){idEstadoUsuario=v;}
    public String getNombreEstado(){return nombreEstado;} public void setNombreEstado(String v){nombreEstado=v;}
    public String getFoto(){return foto;} public void setFoto(String v){foto=v;}
    public Integer getIdTipoDocumento(){return idTipoDocumento;} public void setIdTipoDocumento(Integer v){idTipoDocumento=v;}
}
