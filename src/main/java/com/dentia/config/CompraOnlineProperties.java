package com.dentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dentia.compra-online")
public class CompraOnlineProperties {

    private int reservaMinutos = 7;
    private long schedulerExpiracionMs = 60000;
    private int schedulerExpiracionLimite = 100;
    private int ordenCreadaExpiracionMinutos = 60;
    private long schedulerOrdenesAbandonadasMs = 60000;
    private int schedulerOrdenesAbandonadasLimite = 100;

    public int getReservaMinutos() { return reservaMinutos; }
    public void setReservaMinutos(int reservaMinutos) { this.reservaMinutos = reservaMinutos; }
    public long getSchedulerExpiracionMs() { return schedulerExpiracionMs; }
    public void setSchedulerExpiracionMs(long schedulerExpiracionMs) { this.schedulerExpiracionMs = schedulerExpiracionMs; }
    public int getSchedulerExpiracionLimite() { return schedulerExpiracionLimite; }
    public void setSchedulerExpiracionLimite(int schedulerExpiracionLimite) { this.schedulerExpiracionLimite = schedulerExpiracionLimite; }
    public int getOrdenCreadaExpiracionMinutos() { return ordenCreadaExpiracionMinutos; }
    public void setOrdenCreadaExpiracionMinutos(int ordenCreadaExpiracionMinutos) { this.ordenCreadaExpiracionMinutos = ordenCreadaExpiracionMinutos; }
    public long getSchedulerOrdenesAbandonadasMs() { return schedulerOrdenesAbandonadasMs; }
    public void setSchedulerOrdenesAbandonadasMs(long schedulerOrdenesAbandonadasMs) { this.schedulerOrdenesAbandonadasMs = schedulerOrdenesAbandonadasMs; }
    public int getSchedulerOrdenesAbandonadasLimite() { return schedulerOrdenesAbandonadasLimite; }
    public void setSchedulerOrdenesAbandonadasLimite(int schedulerOrdenesAbandonadasLimite) { this.schedulerOrdenesAbandonadasLimite = schedulerOrdenesAbandonadasLimite; }
}
