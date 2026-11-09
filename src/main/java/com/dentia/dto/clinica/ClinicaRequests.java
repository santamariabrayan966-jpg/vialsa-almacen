package com.dentia.dto.clinica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ClinicaRequests {
    private ClinicaRequests() {}

    public record HistoriaInput(
            Long pacienteId,
            String alergias,
            String enfermedadesSistemicas,
            String medicacionActual,
            Boolean hemorragias,
            Boolean embarazo,
            String antecedentesFamiliares,
            String habitos,
            String motivoPrimeraConsulta,
            String grupoSanguineo,
            String presionArterial,
            String observacionesMedicas
    ) {}

    public record OdontogramaPiezaInput(
            Long odontogramaId,
            Integer numeroPieza,
            String superficie,
            String condicion,
            String estado,
            String notas,
            Long version
    ) {}

    public record LimpiarPiezaInput(Long odontogramaId, Integer numeroPieza, Long version) {}
    public record NotaPiezaInput(Long odontogramaId, Integer numeroPieza, Long version, String nota) {}
    public record PosicionPiezaInput(Integer numeroPieza, Double inclinacion, String presencia) {}
    public record ConfiguracionOdontogramaInput(Long odontogramaId, Long version, List<PosicionPiezaInput> piezas) {}
    public record BorradorAtencionInput(Long pacienteId, Long version, AtencionInput contenido) {}
    public record FinalizarAtencionInput(Long version, String solicitudId, AtencionInput contenido) {}

    public record DiagnosticoInput(
            Long pacienteId,
            Long diagnosticoCatalogoId,
            String codigoCie10,
            String descripcion,
            Integer numeroPieza,
            String gravedad,
            String estado,
            LocalDate fechaDiagnostico,
            String observaciones
    ) {}

    public record TratamientoInput(
            String codigo,
            String nombre,
            String categoria,
            String subcategoria,
            String descripcion,
            BigDecimal precioSugerido,
            Integer duracionEstimadaMin,
            Boolean activo
    ) {}

    public record PlanItemInput(
            Long id,
            Long tratamientoCatalogoId,
            Long diagnosticoId,
            Integer numeroPieza,
            String descripcion,
            BigDecimal precio,
            Integer ordenFase,
            String estado
    ) {}

    public record PlanTratamientoInput(
            Long pacienteId,
            String titulo,
            String estado,
            String notas,
            List<PlanItemInput> items
    ) {}

    public record AtencionInput(
            Long pacienteId,
            Long profesionalId,
            Long citaId,
            LocalDateTime fechaAtencion,
            String motivoConsulta,
            String subjetivo,
            String objetivo,
            String evaluacion,
            String planProcedimiento,
            String piezasTratadas,
            String anestesiaUtilizada,
            String materiales,
            String indicacionesReceta,
            String proximaCitaSugerida,
            Long itemPlanIdCompletar,
            BigDecimal importeTratamiento
    ) {
        public AtencionInput(Long pacienteId, Long profesionalId, Long citaId, LocalDateTime fechaAtencion,
                String motivoConsulta, String subjetivo, String objetivo, String evaluacion, String planProcedimiento,
                String piezasTratadas, String anestesiaUtilizada, String materiales, String indicacionesReceta,
                String proximaCitaSugerida, Long itemPlanIdCompletar) {
            this(pacienteId, profesionalId, citaId, fechaAtencion, motivoConsulta, subjetivo, objetivo, evaluacion,
                planProcedimiento, piezasTratadas, anestesiaUtilizada, materiales, indicacionesReceta, proximaCitaSugerida,
                itemPlanIdCompletar, null);
        }
    }

    public record EstadoInput(
            String estado,
            String notas
    ) {}

    public record FiltroClinico(
            Long paciente,
            String q,
            String estado,
            LocalDate desde,
            LocalDate hasta,
            int pagina
    ) {}
}
