package com.vialsa.almacen.model;

public class Permiso {

    private int idPermiso;
    private int idRol;
    private String modulo;
    private boolean puedeAcceder;

    // --- Getters y Setters ---
    public int getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(int idPermiso) {
        this.idPermiso = idPermiso;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public boolean isPuedeAcceder() {
        return puedeAcceder;
    }

    public void setPuedeAcceder(boolean puedeAcceder) {
        this.puedeAcceder = puedeAcceder;
    }
}
