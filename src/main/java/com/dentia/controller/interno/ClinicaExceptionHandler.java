package com.dentia.controller.interno;

import com.dentia.service.clinica.OdontogramaService.EdicionConcurrenteException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = ClinicaInternaController.class)
public class ClinicaExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalido(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage())); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> jsonInvalido() { return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisa los datos enviados.")); }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> ausente(NoSuchElementException e) { return ResponseEntity.status(404).body(Map.of("mensaje", e.getMessage())); }
    @ExceptionHandler(EdicionConcurrenteException.class)
    ResponseEntity<?> conflicto(EdicionConcurrenteException e) { return ResponseEntity.status(409).body(Map.of("mensaje", e.getMessage())); }
}
