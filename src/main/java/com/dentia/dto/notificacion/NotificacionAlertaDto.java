package com.dentia.dto.notificacion;

import java.time.LocalDateTime;

public class NotificacionAlertaDto {
    private String id;
    private Long idCita;
    private Long idPaciente;
    private String pacienteNombre;
    private String pacienteCelular;
    private String profesionalNombre;
    private String tipoAtencion;
    private LocalDateTime fechaHoraInicio;
    private String tipo; // NO_ASISTIO, HOY, MANANA
    private String titulo;
    private String mensaje;
    private String mensajeSugeridoWhatsApp;
    private String badgeColor; // danger, warning, info
    private String url;

    public NotificacionAlertaDto() {}

    public NotificacionAlertaDto(String id, Long idCita, Long idPaciente, String pacienteNombre,
                                 String pacienteCelular, String profesionalNombre, String tipoAtencion,
                                 LocalDateTime fechaHoraInicio, String tipo, String titulo,
                                 String mensaje, String mensajeSugeridoWhatsApp, String badgeColor, String url) {
        this.id = id;
        this.idCita = idCita;
        this.idPaciente = idPaciente;
        this.pacienteNombre = pacienteNombre;
        this.pacienteCelular = pacienteCelular;
        this.profesionalNombre = profesionalNombre;
        this.tipoAtencion = tipoAtencion;
        this.fechaHoraInicio = fechaHoraInicio;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.mensajeSugeridoWhatsApp = mensajeSugeridoWhatsApp;
        this.badgeColor = badgeColor;
        this.url = url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getIdCita() { return idCita; }
    public void setIdCita(Long idCita) { this.idCita = idCita; }

    public Long getIdPaciente() { return idPaciente; }
    public void setIdPaciente(Long idPaciente) { this.idPaciente = idPaciente; }

    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String pacienteNombre) { this.pacienteNombre = pacienteNombre; }

    public String getPacienteCelular() { return pacienteCelular; }
    public void setPacienteCelular(String pacienteCelular) { this.pacienteCelular = pacienteCelular; }

    public String getProfesionalNombre() { return profesionalNombre; }
    public void setProfesionalNombre(String profesionalNombre) { this.profesionalNombre = profesionalNombre; }

    public String getTipoAtencion() { return tipoAtencion; }
    public void setTipoAtencion(String tipoAtencion) { this.tipoAtencion = tipoAtencion; }

    public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getMensajeSugeridoWhatsApp() { return mensajeSugeridoWhatsApp; }
    public void setMensajeSugeridoWhatsApp(String mensajeSugeridoWhatsApp) { this.mensajeSugeridoWhatsApp = mensajeSugeridoWhatsApp; }

    public String getBadgeColor() { return badgeColor; }
    public void setBadgeColor(String badgeColor) { this.badgeColor = badgeColor; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
