package com.dentia.exception;

public class CompraOnlineException extends RuntimeException {

    private final String codigo;

    public CompraOnlineException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
