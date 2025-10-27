package com.vialsa.almacen.model;
public class Usuario {
  private Long idUsuario; private String nombreUsuario; private String contrasena; private Integer idRol;
  public Long getIdUsuario(){return idUsuario;} public void setIdUsuario(Long v){idUsuario=v;}
  public String getNombreUsuario(){return nombreUsuario;} public void setNombreUsuario(String v){nombreUsuario=v;}
  public String getContrasena(){return contrasena;} public void setContrasena(String v){contrasena=v;}
  public Integer getIdRol(){return idRol;} public void setIdRol(Integer v){idRol=v;}
}
