package com.dentia.model.interno;

import java.time.LocalDateTime;

public class MarcaIdentidadConfig {

    private Integer idConfig;

    private String nombreComercial;
    private String razonSocial;
    private String ruc;
    private String slogan;
    private String descripcionCorta;

    private String logoPrincipal;
    private String logoClaro;
    private String logoOscuro;
    private String isotipo;
    private String favicon;

    // NUEVO: 3 opciones de logo
    private String logoOpcion1;
    private String logoOpcion2;
    private String logoOpcion3;
    private Integer logoActivo;

    private String colorPrimario;
    private String colorSecundario;
    private String colorAcento;
    private String colorNeutroOscuro;
    private String colorNeutroMedio;
    private String colorNeutroClaro;

    private String whatsappPrincipal;
    private String telefonoPrincipal;
    private String correoContacto;
    private String sitioWeb;
    private String direccionPrincipal;
    private String horarioGeneral;
    private String nombreDocumentos;
    private String pieDocumentos;

    private String facebookUrl;
    private String instagramUrl;
    private String tiktokUrl;
    private String linkedinUrl;

    private Boolean activo;
    private Boolean publicado;
    private LocalDateTime fechaActualizacion;

    public MarcaIdentidadConfig() {
    }

    public Integer getIdConfig() {
        return idConfig;
    }

    public void setIdConfig(Integer idConfig) {
        this.idConfig = idConfig;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan;
    }

    public String getDescripcionCorta() {
        return descripcionCorta;
    }

    public void setDescripcionCorta(String descripcionCorta) {
        this.descripcionCorta = descripcionCorta;
    }

    public String getLogoPrincipal() {
        return logoPrincipal;
    }

    public void setLogoPrincipal(String logoPrincipal) {
        this.logoPrincipal = logoPrincipal;
    }

    public String getLogoClaro() {
        return logoClaro;
    }

    public void setLogoClaro(String logoClaro) {
        this.logoClaro = logoClaro;
    }

    public String getLogoOscuro() {
        return logoOscuro;
    }

    public void setLogoOscuro(String logoOscuro) {
        this.logoOscuro = logoOscuro;
    }

    public String getIsotipo() {
        return isotipo;
    }

    public void setIsotipo(String isotipo) {
        this.isotipo = isotipo;
    }

    public String getFavicon() {
        return favicon;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public String getLogoOpcion1() {
        return logoOpcion1;
    }

    public void setLogoOpcion1(String logoOpcion1) {
        this.logoOpcion1 = logoOpcion1;
    }

    public String getLogoOpcion2() {
        return logoOpcion2;
    }

    public void setLogoOpcion2(String logoOpcion2) {
        this.logoOpcion2 = logoOpcion2;
    }

    public String getLogoOpcion3() {
        return logoOpcion3;
    }

    public void setLogoOpcion3(String logoOpcion3) {
        this.logoOpcion3 = logoOpcion3;
    }

    public Integer getLogoActivo() {
        return logoActivo;
    }

    public void setLogoActivo(Integer logoActivo) {
        this.logoActivo = logoActivo;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public String getColorAcento() {
        return colorAcento;
    }

    public void setColorAcento(String colorAcento) {
        this.colorAcento = colorAcento;
    }

    public String getColorNeutroOscuro() {
        return colorNeutroOscuro;
    }

    public void setColorNeutroOscuro(String colorNeutroOscuro) {
        this.colorNeutroOscuro = colorNeutroOscuro;
    }

    public String getColorNeutroMedio() {
        return colorNeutroMedio;
    }

    public void setColorNeutroMedio(String colorNeutroMedio) {
        this.colorNeutroMedio = colorNeutroMedio;
    }

    public String getColorNeutroClaro() {
        return colorNeutroClaro;
    }

    public void setColorNeutroClaro(String colorNeutroClaro) {
        this.colorNeutroClaro = colorNeutroClaro;
    }

    public String getWhatsappPrincipal() {
        return whatsappPrincipal;
    }

    public void setWhatsappPrincipal(String whatsappPrincipal) {
        this.whatsappPrincipal = whatsappPrincipal;
    }

    public String getTelefonoPrincipal() {
        return telefonoPrincipal;
    }

    public void setTelefonoPrincipal(String telefonoPrincipal) {
        this.telefonoPrincipal = telefonoPrincipal;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public void setSitioWeb(String sitioWeb) {
        this.sitioWeb = sitioWeb;
    }

    public String getDireccionPrincipal() {
        return direccionPrincipal;
    }

    public void setDireccionPrincipal(String direccionPrincipal) {
        this.direccionPrincipal = direccionPrincipal;
    }

    public String getHorarioGeneral() { return horarioGeneral; }
    public void setHorarioGeneral(String horarioGeneral) { this.horarioGeneral = horarioGeneral; }
    public String getNombreDocumentos() { return nombreDocumentos; }
    public void setNombreDocumentos(String nombreDocumentos) { this.nombreDocumentos = nombreDocumentos; }
    public String getPieDocumentos() { return pieDocumentos; }
    public void setPieDocumentos(String pieDocumentos) { this.pieDocumentos = pieDocumentos; }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getTiktokUrl() {
        return tiktokUrl;
    }

    public void setTiktokUrl(String tiktokUrl) {
        this.tiktokUrl = tiktokUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getPublicado() {
        return publicado;
    }

    public void setPublicado(Boolean publicado) {
        this.publicado = publicado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String obtenerLogoActivo() {
        if (logoActivo == null) {
            return logoPrincipal;
        }

        if (logoActivo == 1) {
            return logoOpcion1 != null && !logoOpcion1.isBlank() ? logoOpcion1 : logoPrincipal;
        }

        if (logoActivo == 2) {
            return logoOpcion2 != null && !logoOpcion2.isBlank() ? logoOpcion2 : logoPrincipal;
        }

        if (logoActivo == 3) {
            return logoOpcion3 != null && !logoOpcion3.isBlank() ? logoOpcion3 : logoPrincipal;
        }

        return logoPrincipal;
    }
}
