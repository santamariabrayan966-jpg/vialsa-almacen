package com.dentia.dto.gestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class GestionRequests {
    private GestionRequests() {}
    public record Item(String descripcion, Integer cantidad, BigDecimal precio) {}
    public record Presupuesto(Long pacienteId, String titulo, LocalDate vence, String notas,
                              List<Item> items, Integer version) {}
    public record Pago(Long presupuestoId, BigDecimal monto, LocalDate fecha, String metodo,
                       String referencia, String token) {}
    public record Seguimiento(Long pacienteId, Long citaId, Long presupuestoId, String titulo,
                              LocalDateTime fecha, String canal, String notas, Integer version) {}
    public record Estado(String estado, String motivo, Integer version) {}
    public record Filtro(Long paciente, String q, String estado, LocalDate desde, LocalDate hasta, int pagina) {}
}
