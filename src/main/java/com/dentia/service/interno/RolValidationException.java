package com.dentia.service.interno;

public class RolValidationException extends RuntimeException {
    public enum Codigo { NOMBRE_REQUERIDO, NOMBRE_DUPLICADO, ADMIN_PROTEGIDO, USUARIOS_ASOCIADOS, NO_ENCONTRADO }
    private final Codigo codigo;

    public RolValidationException(Codigo codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public Codigo getCodigo() { return codigo; }
}
