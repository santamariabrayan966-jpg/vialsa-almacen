package com.dentia.service.clinica;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.dto.clinica.ClinicaRequests.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/** Ciclo transaccional de borrador y cierre. Todas las escrituras se serializan por paciente. */
@Service
public class AtencionClinicaService {
    private final JdbcClinicaDao dao;
    private final ObjectMapper json;

    public AtencionClinicaService(JdbcClinicaDao dao, ObjectMapper json) {
        this.dao = dao;
        this.json = json;
    }

    public Map<String, Object> obtenerBorrador(long pacienteId) {
        var row = dao.unoOpcional("SELECT contenido, version, actualizado_en actualizadoEn FROM clinica_atencion_borradores WHERE paciente_id = ?", pacienteId);
        if (row == null) return Map.of("version", 0);
        row = new HashMap<>(row);
        try {
            row.put("contenido", json.readTree(String.valueOf(row.get("contenido"))));
            return row;
        } catch (JsonProcessingException e) { throw new IllegalStateException("No se pudo leer el borrador guardado.", e); }
    }

    @Transactional
    public Map<String, Object> guardarBorrador(BorradorAtencionInput input, String actor) {
        if (input == null || input.pacienteId() == null || input.contenido() == null
                || !input.pacienteId().equals(input.contenido().pacienteId())) throw new IllegalArgumentException("Paciente del borrador inválido.");
        bloquearPaciente(input.pacienteId());
        var actual = obtenerBorrador(input.pacienteId());
        long version = ((Number) actual.get("version")).longValue();
        if (input.version() == null || input.version() != version) throw new OdontogramaService.EdicionConcurrenteException();
        try {
            String contenido = json.writeValueAsString(input.contenido());
            if (contenido.length() > 20000) throw new IllegalArgumentException("El borrador es demasiado extenso.");
            dao.actualizar("""
                INSERT INTO clinica_atencion_borradores (paciente_id, contenido, version, actualizado_por) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE contenido=VALUES(contenido), version=VALUES(version), actualizado_por=VALUES(actualizado_por)
                """, input.pacienteId(), contenido, version + 1, actor != null ? actor : "SISTEMA");
        } catch (JsonProcessingException e) { throw new IllegalArgumentException("No se pudo guardar el borrador."); }
        return Map.of("ok", true, "version", version + 1);
    }

    @Transactional
    public long registrarAtencion(FinalizarAtencionInput solicitud, String actor) {
        if (solicitud == null || solicitud.contenido() == null || solicitud.version() == null || solicitud.version() < 0)
            throw new IllegalArgumentException("Se requiere el contenido y la versión de la atención.");
        if (solicitud.solicitudId() == null || !solicitud.solicitudId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw new IllegalArgumentException("Se requiere un identificador de solicitud UUID.");
        AtencionInput input = solicitud.contenido();
        if (input.pacienteId() == null) throw new IllegalArgumentException("Se requiere el paciente.");
        if (input.planProcedimiento() == null || input.planProcedimiento().isBlank()) {
            throw new IllegalArgumentException("El detalle del procedimiento realizado es obligatorio.");
        }

        if (input.importeTratamiento() != null && (input.importeTratamiento().signum() < 0 || input.importeTratamiento().scale() > 2 || input.importeTratamiento().compareTo(new java.math.BigDecimal("9999999.99")) > 0))
            throw new IllegalArgumentException("El importe del tratamiento debe ser válido, positivo y tener como máximo dos decimales.");
        bloquearPaciente(input.pacienteId());
        String huella = huella(solicitud, actor);
        var anterior = dao.unoOpcional("SELECT atencion_id, huella FROM clinica_atencion_solicitudes WHERE paciente_id=? AND solicitud_id=?",
                input.pacienteId(), solicitud.solicitudId());
        // Consultar primero el recibo: un reintento válido no toca un borrador posterior.
        if (anterior != null) {
            if (!huella.equals(anterior.get("huella"))) throw new OdontogramaService.EdicionConcurrenteException();
            return ((Number) anterior.get("atencion_id")).longValue();
        }
        long version = ((Number) obtenerBorrador(input.pacienteId()).get("version")).longValue();
        if (solicitud.version() != version) throw new OdontogramaService.EdicionConcurrenteException();
        if (input.citaId() != null) {
            var cita = dao.unoOpcional("SELECT id_paciente, estado FROM citas WHERE id_cita=? FOR UPDATE", input.citaId());
            if (cita == null || ((Number) cita.get("id_paciente")).longValue() != input.pacienteId())
                throw new IllegalArgumentException("La cita no pertenece al paciente.");
            if (Set.of("ATENDIDA", "CANCELADA", "NO_ASISTIO").contains(String.valueOf(cita.get("estado"))))
                throw new IllegalArgumentException("Esta cita no admite una nueva atención.");
            var anteriorCita = dao.unoOpcional("SELECT id FROM clinica_atenciones WHERE cita_id=?", input.citaId());
            if (anteriorCita != null && anteriorCita.get("id") != null)
                throw new IllegalArgumentException("Esta cita ya cuenta con una atención clínica registrada.");
        }
        if (input.itemPlanIdCompletar() != null) {
            var item = dao.unoOpcional("SELECT p.paciente_id, i.estado FROM clinica_plan_tratamiento_items i JOIN clinica_planes_tratamiento p ON p.id=i.plan_id WHERE i.id=? FOR UPDATE", input.itemPlanIdCompletar());
            if (item == null || ((Number) item.get("paciente_id")).longValue() != input.pacienteId())
                throw new IllegalArgumentException("El procedimiento no pertenece al paciente.");
            if (Set.of("REALIZADO", "CANCELADO").contains(String.valueOf(item.get("estado"))))
                throw new IllegalArgumentException("El procedimiento ya está cerrado.");
        }

        LocalDateTime fecha = input.fechaAtencion() != null ? input.fechaAtencion() : LocalDateTime.now();
        String motivo = (input.motivoConsulta() == null || input.motivoConsulta().isBlank()) ? "Control y tratamiento" : input.motivoConsulta().trim();

        long atencionId = dao.insertarAtencion(
                input.pacienteId(),
                input.profesionalId(),
                input.citaId(),
                fecha.toString(),
                validarLongitud(motivo, "Motivo de consulta", 500),
                validarLongitud(input.subjetivo(), "Relato subjetivo", 1500),
                validarLongitud(input.objetivo(), "Hallazgos objetivos", 1500),
                validarLongitud(input.evaluacion(), "Evaluación", 1500),
                validarLongitud(input.planProcedimiento(), "Procedimiento realizado", 2000),
                validarLongitud(input.piezasTratadas(), "Piezas tratadas", 100),
                validarLongitud(input.anestesiaUtilizada(), "Anestesia utilizada", 200),
                validarLongitud(input.materiales(), "Materiales", 500),
                validarLongitud(input.indicacionesReceta(), "Indicaciones y receta", 2000),
                validarLongitud(input.proximaCitaSugerida(), "Próxima cita sugerida", 300),
                actor != null ? actor : "SISTEMA"
        );

        // Si se indicó un item del plan de tratamiento para completar, lo marcamos como REALIZADO
        if (input.itemPlanIdCompletar() != null) {
            dao.actualizarEstadoItemPlan(input.itemPlanIdCompletar(), "REALIZADO", atencionId);
            dao.actualizar("UPDATE clinica_planes_tratamiento p JOIN clinica_plan_tratamiento_items elegido ON elegido.plan_id=p.id SET p.estado=CASE WHEN EXISTS (SELECT 1 FROM clinica_plan_tratamiento_items pendiente WHERE pendiente.plan_id=p.id AND pendiente.estado NOT IN ('REALIZADO','CANCELADO')) THEN 'EN_EJECUCION' ELSE 'COMPLETADO' END WHERE elegido.id=? AND p.estado IN ('APROBADO','EN_EJECUCION')", input.itemPlanIdCompletar());
        }

        // Si se vinculó una cita, actualizar su estado en la agenda a 'ATENDIDA'
        if (input.citaId() != null) {
            dao.actualizar("UPDATE citas SET estado = 'ATENDIDA' WHERE id_cita = ?", input.citaId());
            dao.actualizar("""
                INSERT INTO cita_historial (id_cita, estado_anterior, estado_nuevo, observacion, tipo_evento)
                VALUES (?, 'EN_ATENCION', 'ATENDIDA', 'Atención clínica finalizada', 'CAMBIO_ESTADO')
                """, input.citaId());
        }

        // Auto-evolucionar odontograma según el tratamiento y piezas tratadas
        evolucionarOdontogramaPorAtencion(input.pacienteId(), input.piezasTratadas(), input.planProcedimiento(), actor);

        // El cargo de una atención directa se genera una sola vez en la misma transacción.
        // Un procedimiento vinculado a un plan ya cuenta con su presupuesto y no se vuelve a cobrar.
        if (input.itemPlanIdCompletar() == null && input.importeTratamiento() != null && input.importeTratamiento().signum() > 0) {
            long presupuestoId = dao.insertar("INSERT INTO gestion_presupuestos (paciente_id,titulo,fecha,vence,estado,total,notas,creado_por) VALUES (?,?,?,?,'APROBADO',?,?,?)",
                    input.pacienteId(), "Atención clínica #" + atencionId, fecha.toLocalDate(), fecha.toLocalDate().plusDays(30),
                    input.importeTratamiento(), "Importe acordado para la atención #" + atencionId + ". Pendiente de pago.", actor != null ? actor : "SISTEMA");
            String concepto = input.planProcedimiento().trim();
            if (concepto.length() > 250) concepto = concepto.substring(0,250);
            dao.insertar("INSERT INTO gestion_presupuesto_items (presupuesto_id,descripcion,cantidad,precio) VALUES (?,?,1,?)", presupuestoId, concepto, input.importeTratamiento());
            dao.insertar("INSERT INTO gestion_historial (modulo,registro_id,accion,detalle,usuario) VALUES ('presupuestos',?,'ATENCION',?,?)", presupuestoId,
                    "Cargo vinculado a atención #" + atencionId, actor != null ? actor : "SISTEMA");
        }

        // Mantener la revisión como marca de cierre evita que una sesión antigua recree el borrador.
        dao.actualizar("""
                INSERT INTO clinica_atencion_borradores (paciente_id, contenido, version, actualizado_por) VALUES (?, 'null', ?, ?)
                ON DUPLICATE KEY UPDATE contenido='null', version=VALUES(version), actualizado_por=VALUES(actualizado_por)
                """, input.pacienteId(), version + 1, actor != null ? actor : "SISTEMA");
        dao.actualizar("INSERT INTO clinica_atencion_solicitudes (paciente_id, solicitud_id, atencion_id, huella) VALUES (?, ?, ?, ?)",
                input.pacienteId(), solicitud.solicitudId(), atencionId, huella);
        return atencionId;
    }

    private void evolucionarOdontogramaPorAtencion(Long pacienteId, String piezasTratadas, String procedimiento, String actor) {
        if (pacienteId == null || piezasTratadas == null || piezasTratadas.isBlank()) return;

        var matcher = java.util.regex.Pattern.compile("\\b(1[1-8]|2[1-8]|3[1-8]|4[1-8]|5[1-5]|6[1-5]|7[1-5]|8[1-5])\\b")
                .matcher(piezasTratadas);
        List<Integer> piezas = new ArrayList<>();
        while (matcher.find()) {
            piezas.add(Integer.parseInt(matcher.group()));
        }
        if (piezas.isEmpty()) return;

        String proc = procedimiento != null ? procedimiento.toLowerCase(Locale.ROOT) : "";
        boolean esResina = proc.contains("resina") || proc.contains("curaci") || proc.contains("obturaci") || proc.contains("restauraci");
        boolean esEndodoncia = proc.contains("endodoncia") || proc.contains("conducto") || proc.contains("pulpectom");
        boolean esCorona = proc.contains("corona") || proc.contains("funda") || proc.contains("prótesis") || proc.contains("protesis");
        boolean esSellante = proc.contains("sellante");
        boolean esExodoncia = proc.contains("exodoncia") || proc.contains("extracci");
        boolean esImplante = proc.contains("implante");
        boolean esBrackets = proc.contains("bracket") || proc.contains("ortodoncia");

        String nuevaCondicion = "OBTURACION";
        if (esEndodoncia) nuevaCondicion = "ENDODONCIA";
        else if (esCorona) nuevaCondicion = "CORONA";
        else if (esSellante) nuevaCondicion = "SELLANTE";
        else if (esExodoncia) nuevaCondicion = "AUSENTE";
        else if (esImplante) nuevaCondicion = "IMPLANTE";
        else if (esBrackets) nuevaCondicion = "BRACKETS";
        else if (!esResina) {
            return;
        }

        try {
            var odonto = dao.obtenerOdontogramaActivo(pacienteId, "INICIAL");
            if (odonto == null || odonto.get("id") == null) return;
            long odontoId = ((Number) odonto.get("id")).longValue();

            List<Map<String, Object>> piezasActuales = dao.listarPiezasOdontograma(odontoId);
            boolean huboCambios = false;

            for (int pieza : piezas) {
                List<Map<String, Object>> marcasDiente = piezasActuales != null ? piezasActuales.stream()
                        .filter(m -> {
                            Object np = m.get("numeroPieza");
                            return np != null && ((Number) np).intValue() == pieza;
                        })
                        .toList() : Collections.emptyList();

                if ("AUSENTE".equals(nuevaCondicion)) {
                    dao.limpiarPiezaOdontograma(odontoId, pieza);
                    dao.guardarPiezaOdontograma(odontoId, pieza, "GENERAL", "AUSENTE", "REALIZADO", "Exodoncia realizada: " + (procedimiento != null ? procedimiento : ""));
                    dao.actualizar("INSERT INTO clinica_odontograma_historial (odontograma_id, numero_pieza, superficie, accion, condicion, notas, usuario) VALUES (?, ?, 'GENERAL', 'ATENCION', 'AUSENTE', ?, ?)",
                            odontoId, pieza, "Atención clínica: " + (procedimiento != null ? procedimiento : ""), actor != null ? actor : "SISTEMA");
                    huboCambios = true;
                } else if ("ENDODONCIA".equals(nuevaCondicion) || "CORONA".equals(nuevaCondicion) || "IMPLANTE".equals(nuevaCondicion) || "BRACKETS".equals(nuevaCondicion)) {
                    dao.guardarPiezaOdontograma(odontoId, pieza, "GENERAL", nuevaCondicion, "REALIZADO", "Tratamiento realizado: " + (procedimiento != null ? procedimiento : ""));
                    dao.actualizar("INSERT INTO clinica_odontograma_historial (odontograma_id, numero_pieza, superficie, accion, condicion, notas, usuario) VALUES (?, ?, 'GENERAL', 'ATENCION', ?, ?, ?)",
                            odontoId, pieza, nuevaCondicion, "Atención clínica: " + (procedimiento != null ? procedimiento : ""), actor != null ? actor : "SISTEMA");
                    huboCambios = true;
                } else {
                    List<Map<String, Object>> caries = marcasDiente.stream()
                            .filter(m -> "CARIES".equalsIgnoreCase(String.valueOf(m.get("condicion"))))
                            .toList();

                    if (!caries.isEmpty()) {
                        for (var c : caries) {
                            String sup = c.get("superficie") != null ? String.valueOf(c.get("superficie")) : "GENERAL";
                            dao.guardarPiezaOdontograma(odontoId, pieza, sup, nuevaCondicion, "REALIZADO", "Curación realizada: " + (procedimiento != null ? procedimiento : ""));
                            dao.actualizar("INSERT INTO clinica_odontograma_historial (odontograma_id, numero_pieza, superficie, accion, condicion, notas, usuario) VALUES (?, ?, ?, 'ATENCION', ?, ?, ?)",
                                    odontoId, pieza, sup, nuevaCondicion, "Atención clínica: " + (procedimiento != null ? procedimiento : ""), actor != null ? actor : "SISTEMA");
                            huboCambios = true;
                        }
                    } else {
                        dao.guardarPiezaOdontograma(odontoId, pieza, "GENERAL", nuevaCondicion, "REALIZADO", "Tratamiento realizado: " + (procedimiento != null ? procedimiento : ""));
                        dao.actualizar("INSERT INTO clinica_odontograma_historial (odontograma_id, numero_pieza, superficie, accion, condicion, notas, usuario) VALUES (?, ?, 'GENERAL', 'ATENCION', ?, ?, ?)",
                                odontoId, pieza, nuevaCondicion, "Atención clínica: " + (procedimiento != null ? procedimiento : ""), actor != null ? actor : "SISTEMA");
                        huboCambios = true;
                    }
                }
            }

            if (huboCambios) {
                dao.actualizar("INSERT IGNORE INTO clinica_odontograma_versiones (odontograma_id) VALUES (?)", odontoId);
                dao.actualizar("UPDATE clinica_odontograma_versiones SET version = version + 1 WHERE odontograma_id = ?", odontoId);
            }
        } catch (org.springframework.dao.DataAccessException e) {
            throw new IllegalStateException("No se pudo actualizar el odontograma. La atención no se ha guardado; vuelve a intentarlo.", e);
        }
    }

    private void bloquearPaciente(Long pacienteId) {
        if (dao.unoOpcional("SELECT id_paciente FROM pacientes WHERE id_paciente=? FOR UPDATE", pacienteId) == null)
            throw new NoSuchElementException("Paciente no encontrado.");
    }

    private String huella(FinalizarAtencionInput solicitud, String actor) {
        try {
            String payload = json.writeValueAsString(solicitud) + "\n" + (actor != null ? actor : "SISTEMA");
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo leer la atención.");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    private static String validarLongitud(String valor, String campo, int max) {
        if (valor == null) return null;
        String texto = valor.trim();
        if (texto.length() > max) throw new IllegalArgumentException(campo + " supera el límite de " + max + " caracteres.");
        return texto;
    }
}
