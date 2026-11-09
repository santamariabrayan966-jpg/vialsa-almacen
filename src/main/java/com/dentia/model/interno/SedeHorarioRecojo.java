package com.dentia.model.interno;

import java.time.LocalTime;

public class SedeHorarioRecojo {

    private Integer idHorarioRecojo;
    private Integer idSede;
    private String nombreBloque;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer duracionFranjaMinutos;
    private Integer capacidadPorFranja;
    private Boolean activo;

    public Integer getIdHorarioRecojo() { return idHorarioRecojo; }
    public void setIdHorarioRecojo(Integer idHorarioRecojo) { this.idHorarioRecojo = idHorarioRecojo; }
    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }
    public String getNombreBloque() { return nombreBloque; }
    public void setNombreBloque(String nombreBloque) { this.nombreBloque = nombreBloque; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Integer getDuracionFranjaMinutos() { return duracionFranjaMinutos; }
    public void setDuracionFranjaMinutos(Integer duracionFranjaMinutos) { this.duracionFranjaMinutos = duracionFranjaMinutos; }
    public Integer getCapacidadPorFranja() { return capacidadPorFranja; }
    public void setCapacidadPorFranja(Integer capacidadPorFranja) { this.capacidadPorFranja = capacidadPorFranja; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
