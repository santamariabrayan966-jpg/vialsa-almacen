package com.dentia.model.interno;

import java.time.LocalDateTime;

public class PanelColoresConfig {

    private Integer idConfig;
    private String colorHeader;
    private String colorHeaderSecundario;
    private String colorFondo;
    private String colorTarjeta;
    private String colorBotonPrimario;
    private String colorAcento;
    private String colorTextoPrincipal;
    private String colorTextoHeader;
    private String colorFooter;
    private String colorTextoFooter;
    private String posicionMenu;
    private LocalDateTime fechaActualizacion;

    public Integer getIdConfig() {
        return idConfig;
    }

    public void setIdConfig(Integer idConfig) {
        this.idConfig = idConfig;
    }

    public String getColorHeader() {
        return colorHeader;
    }

    public void setColorHeader(String colorHeader) {
        this.colorHeader = colorHeader;
    }

    public String getColorHeaderSecundario() {
        return colorHeaderSecundario;
    }

    public void setColorHeaderSecundario(String colorHeaderSecundario) {
        this.colorHeaderSecundario = colorHeaderSecundario;
    }

    public String getColorFondo() {
        return colorFondo;
    }

    public void setColorFondo(String colorFondo) {
        this.colorFondo = colorFondo;
    }

    public String getColorTarjeta() {
        return colorTarjeta;
    }

    public void setColorTarjeta(String colorTarjeta) {
        this.colorTarjeta = colorTarjeta;
    }

    public String getColorBotonPrimario() {
        return colorBotonPrimario;
    }

    public void setColorBotonPrimario(String colorBotonPrimario) {
        this.colorBotonPrimario = colorBotonPrimario;
    }

    public String getColorAcento() {
        return colorAcento;
    }

    public void setColorAcento(String colorAcento) {
        this.colorAcento = colorAcento;
    }

    public String getColorTextoPrincipal() {
        return colorTextoPrincipal;
    }

    public void setColorTextoPrincipal(String colorTextoPrincipal) {
        this.colorTextoPrincipal = colorTextoPrincipal;
    }

    public String getColorTextoHeader() {
        return colorTextoHeader;
    }

    public void setColorTextoHeader(String colorTextoHeader) {
        this.colorTextoHeader = colorTextoHeader;
    }

    public String getColorFooter() {
        return colorFooter;
    }

    public void setColorFooter(String colorFooter) {
        this.colorFooter = colorFooter;
    }

    public String getColorTextoFooter() {
        return colorTextoFooter;
    }

    public void setColorTextoFooter(String colorTextoFooter) {
        this.colorTextoFooter = colorTextoFooter;
    }

    public String getPosicionMenu() {
        return posicionMenu;
    }

    public void setPosicionMenu(String posicionMenu) {
        this.posicionMenu = posicionMenu;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
