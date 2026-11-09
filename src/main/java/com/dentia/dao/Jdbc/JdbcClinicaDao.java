package com.dentia.dao.Jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.*;

@Repository
public class JdbcClinicaDao {

    private final JdbcTemplate jdbc;

    public JdbcClinicaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> consultar(String sql, Object... args) {
        return jdbc.query(sql, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Object value = rs.getObject(i);
                if (value instanceof java.sql.Timestamp t) value = t.toLocalDateTime().toString();
                if (value instanceof java.sql.Date d) value = d.toLocalDate().toString();
                row.put(meta.getColumnLabel(i), value);
            }
            return row;
        }, args);
    }

    public Map<String, Object> unoOpcional(String sql, Object... args) {
        var list = consultar(sql, args);
        return list.isEmpty() ? null : list.get(0);
    }

    public long insertar(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public int actualizar(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    // --- PACIENTES Y BUSCADOR ---
    public Map<String, Object> obtenerPaciente(long pacienteId) {
        String sql = """
            SELECT p.id_paciente id, CONCAT(p.nombres, ' ', p.apellidos) nombreCompleto,
                   p.nombres, p.apellidos, p.dni, p.celular, p.correo, p.fecha_nacimiento fechaNacimiento,
                   TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, CURDATE()) edad, p.activo,
                   h.alergias, h.enfermedades_sistemicas enfermedadesSistemicas, h.grupo_sanguineo grupoSanguineo,
                   h.presion_arterial presionArterial, h.hemorragias, h.embarazo
            FROM pacientes p
            LEFT JOIN clinica_historias h ON h.paciente_id = p.id_paciente
            WHERE p.id_paciente = ?
        """;
        return unoOpcional(sql, pacienteId);
    }

    public List<Map<String, Object>> buscarPacientes(String termino) {
        String query = "%" + (termino == null ? "" : termino.trim()) + "%";
        String sql = """
            SELECT p.id_paciente id, CONCAT(p.nombres, ' ', p.apellidos) nombre,
                   p.dni, p.celular, TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, CURDATE()) edad,
                   COALESCE(h.alergias, '') alergias,
                   COALESCE(h.enfermedades_sistemicas, '') enfermedades
            FROM pacientes p
            LEFT JOIN clinica_historias h ON h.paciente_id = p.id_paciente
            WHERE p.activo = 1 AND (CONCAT(p.nombres, ' ', p.apellidos) LIKE ? OR p.dni LIKE ? OR p.celular LIKE ?)
            ORDER BY p.apellidos, p.nombres
            LIMIT 40
        """;
        return consultar(sql, query, query, query);
    }

    // --- HISTORIA CLÍNICA ---
    public Map<String, Object> obtenerHistoria(long pacienteId) {
        String sql = "SELECT * FROM clinica_historias WHERE paciente_id = ?";
        return unoOpcional(sql, pacienteId);
    }

    public void guardarHistoria(long pacienteId, String alergias, String enfermedades, String medicacion,
                                boolean hemorragias, boolean embarazo, String antecedentes, String habitos,
                                String motivo, String grupoSanguineo, String presion, String observaciones, String actor) {
        String sql = """
            INSERT INTO clinica_historias (
                paciente_id, alergias, enfermedades_sistemicas, medicacion_actual,
                hemorragias, embarazo, antecedentes_familiares, habitos,
                motivo_primera_consulta, grupo_sanguineo, presion_arterial,
                observaciones_medicas, creado_por
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                alergias = VALUES(alergias),
                enfermedades_sistemicas = VALUES(enfermedades_sistemicas),
                medicacion_actual = VALUES(medicacion_actual),
                hemorragias = VALUES(hemorragias),
                embarazo = VALUES(embarazo),
                antecedentes_familiares = VALUES(antecedentes_familiares),
                habitos = VALUES(habitos),
                motivo_primera_consulta = VALUES(motivo_primera_consulta),
                grupo_sanguineo = VALUES(grupo_sanguineo),
                presion_arterial = VALUES(presion_arterial),
                observaciones_medicas = VALUES(observaciones_medicas)
        """;
        actualizar(sql, pacienteId, alergias, enfermedades, medicacion,
                hemorragias ? 1 : 0, embarazo ? 1 : 0, antecedentes, habitos,
                motivo, grupoSanguineo, presion, observaciones, actor);
    }

    // --- ODONTOGRAMA ---
    public Map<String, Object> obtenerOdontogramaActivo(long pacienteId, String tipo) {
        String sql = "SELECT * FROM clinica_odontogramas WHERE paciente_id = ? AND tipo = ? ORDER BY id DESC LIMIT 1";
        var odonto = unoOpcional(sql, pacienteId, tipo);
        if (odonto == null) {
            long nuevoId = insertar("INSERT INTO clinica_odontogramas (paciente_id, tipo, fecha, creado_por) VALUES (?, ?, CURDATE(), 'SISTEMA')",
                    pacienteId, tipo);
            return unoOpcional("SELECT * FROM clinica_odontogramas WHERE id = ?", nuevoId);
        }
        return odonto;
    }

    public List<Map<String, Object>> listarPiezasOdontograma(long odontogramaId) {
        String sql = """
            SELECT id, odontograma_id odontogramaId, numero_pieza numeroPieza,
                   superficie, condicion, estado, notas, fecha_registro fechaRegistro
            FROM clinica_odontograma_piezas
            WHERE odontograma_id = ?
            ORDER BY numero_pieza, superficie
        """;
        return consultar(sql, odontogramaId);
    }

    public void guardarPiezaOdontograma(long odontogramaId, int numeroPieza, String superficie,
                                         String condicion, String estado, String notas) {
        // Elimina condición previa en la misma superficie de la pieza para evitar inconsistencias
        actualizar("DELETE FROM clinica_odontograma_piezas WHERE odontograma_id = ? AND numero_pieza = ? AND superficie = ?",
                odontogramaId, numeroPieza, superficie);

        if (!"SANO".equalsIgnoreCase(condicion)) {
            actualizar("""
                INSERT INTO clinica_odontograma_piezas (odontograma_id, numero_pieza, superficie, condicion, estado, notas)
                VALUES (?, ?, ?, ?, ?, ?)
            """, odontogramaId, numeroPieza, superficie, condicion, estado, notas);
        }
    }

    public void limpiarPiezaOdontograma(long odontogramaId, int numeroPieza) {
        actualizar("DELETE FROM clinica_odontograma_piezas WHERE odontograma_id = ? AND numero_pieza = ?",
                odontogramaId, numeroPieza);
    }

    // --- DIAGNÓSTICOS ---
    public List<Map<String, Object>> listarCatalogoDiagnosticos(String categoria, String q) {
        StringBuilder sql = new StringBuilder("SELECT id, codigo_cie10 codigoCie10, nombre, descripcion, categoria FROM clinica_catalogo_diagnosticos WHERE activo = 1");
        List<Object> args = new ArrayList<>();
        if (categoria != null && !categoria.isBlank()) {
            sql.append(" AND categoria = ?");
            args.add(categoria.trim());
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (nombre LIKE ? OR codigo_cie10 LIKE ? OR descripcion LIKE ?)");
            String term = "%" + q.trim() + "%";
            args.add(term); args.add(term); args.add(term);
        }
        sql.append(" ORDER BY codigo_cie10 ASC LIMIT 50");
        return consultar(sql.toString(), args.toArray());
    }

    public List<Map<String, Object>> listarDiagnosticosPaciente(long pacienteId, String estado) {
        StringBuilder sql = new StringBuilder("""
            SELECT d.id, d.paciente_id pacienteId, d.diagnostico_catalogo_id catalogoId,
                   COALESCE(d.codigo_cie10, c.codigo_cie10) codigoCie10,
                   d.descripcion, d.numero_pieza numeroPieza, d.gravedad, d.estado,
                   d.fecha_diagnostico fechaDiagnostico, d.observaciones, d.creado_por creadoPor
            FROM clinica_paciente_diagnosticos d
            LEFT JOIN clinica_catalogo_diagnosticos c ON c.id = d.diagnostico_catalogo_id
            WHERE d.paciente_id = ?
        """);
        List<Object> args = new ArrayList<>();
        args.add(pacienteId);
        if (estado != null && !estado.isBlank()) {
            sql.append(" AND d.estado = ?");
            args.add(estado.trim());
        }
        sql.append(" ORDER BY (d.estado = 'ACTIVO') DESC, (d.estado = 'EN_TRATAMIENTO') DESC, d.fecha_diagnostico DESC");
        return consultar(sql.toString(), args.toArray());
    }

    public long insertarDiagnostico(long pacienteId, Long catalogoId, String codigoCie10, String descripcion,
                                    Integer numeroPieza, String gravedad, String estado, String fecha, String observaciones, String actor) {
        String sql = """
            INSERT INTO clinica_paciente_diagnosticos (
                paciente_id, diagnostico_catalogo_id, codigo_cie10, descripcion,
                numero_pieza, gravedad, estado, fecha_diagnostico, observaciones, creado_por
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        return insertar(sql, pacienteId, catalogoId, codigoCie10, descripcion, numeroPieza,
                gravedad != null ? gravedad : "MODERADA",
                estado != null ? estado : "ACTIVO",
                fecha, observaciones, actor);
    }

    public void actualizarEstadoDiagnostico(long id, String nuevoEstado) {
        actualizar("UPDATE clinica_paciente_diagnosticos SET estado = ? WHERE id = ?", nuevoEstado, id);
    }

    // --- CATÁLOGO DE TRATAMIENTOS ---
    public List<Map<String, Object>> listarCatalogoTratamientos(String categoria, String q) {
        StringBuilder sql = new StringBuilder("SELECT t.id, t.codigo, t.nombre, t.categoria, t.descripcion, COALESCE(d.subcategoria, 'Otros procedimientos') subcategoria, t.precio_sugerido precio, t.duracion_estimada_min duracion FROM clinica_catalogo_tratamientos t LEFT JOIN clinica_servicio_detalles d ON d.tratamiento_id=t.id WHERE t.activo = 1");
        List<Object> args = new ArrayList<>();
        if (categoria != null && !categoria.isBlank()) {
            sql.append(" AND t.categoria = ?");
            args.add(categoria.trim());
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (t.nombre LIKE ? OR t.codigo LIKE ? OR t.descripcion LIKE ? OR d.subcategoria LIKE ?)");
            String term = "%" + q.trim() + "%";
            args.add(term); args.add(term); args.add(term); args.add(term);
        }
        sql.append(" ORDER BY t.categoria, subcategoria, t.nombre LIMIT 200");
        return consultar(sql.toString(), args.toArray());
    }

    public long guardarTratamientoCatalogo(String codigo, String nombre, String categoria, String descripcion,
                                           BigDecimal precio, int duracion) {
        String sql = """
            INSERT INTO clinica_catalogo_tratamientos (codigo, nombre, categoria, descripcion, precio_sugerido, duracion_estimada_min, activo)
            VALUES (?, ?, ?, ?, ?, ?, 1)
        """;
        return insertar(sql, codigo, nombre, categoria, descripcion, precio, duracion);
    }

    // --- PLANES DE TRATAMIENTO ---
    public List<Map<String, Object>> listarPlanesPaciente(long pacienteId) {
        String sql = """
            SELECT p.id, p.paciente_id pacienteId, p.titulo, p.estado, p.costo_total total,
                   p.presupuesto_id presupuestoId, p.notas, p.creado_por creadoPor, p.creado_en creadoEn,
                   COUNT(i.id) totalItems,
                   SUM(CASE WHEN i.estado = 'REALIZADO' THEN 1 ELSE 0 END) itemsRealizados
            FROM clinica_planes_tratamiento p
            LEFT JOIN clinica_plan_tratamiento_items i ON i.plan_id = p.id
            WHERE p.paciente_id = ?
            GROUP BY p.id
            ORDER BY p.id DESC
        """;
        return consultar(sql, pacienteId);
    }

    public Map<String, Object> obtenerPlan(long planId) {
        String sql = """
            SELECT p.*, CONCAT(pac.nombres, ' ', pac.apellidos) nombrePaciente, pac.dni,
                   b.titulo presupuestoTitulo, b.estado presupuestoEstado
            FROM clinica_planes_tratamiento p
            JOIN pacientes pac ON pac.id_paciente = p.paciente_id
            LEFT JOIN gestion_presupuestos b ON b.id = p.presupuesto_id
            WHERE p.id = ?
        """;
        return unoOpcional(sql, planId);
    }

    /**
     * Obtiene un plan bloqueando su fila durante la generación del presupuesto.
     * El bloqueo serializa dos solicitudes simultáneas y permite reutilizar
     * el presupuesto ya vinculado en lugar de crear duplicados.
     */
    public Map<String, Object> obtenerPlanParaGenerarPresupuesto(long planId) {
        String sql = """
            SELECT p.*, CONCAT(pac.nombres, ' ', pac.apellidos) nombrePaciente, pac.dni,
                   b.titulo presupuestoTitulo, b.estado presupuestoEstado
            FROM clinica_planes_tratamiento p
            JOIN pacientes pac ON pac.id_paciente = p.paciente_id
            LEFT JOIN gestion_presupuestos b ON b.id = p.presupuesto_id
            WHERE p.id = ?
            FOR UPDATE
        """;
        return unoOpcional(sql, planId);
    }

    public List<Map<String, Object>> listarItemsPlan(long planId) {
        String sql = """
            SELECT i.id, i.plan_id planId, i.tratamiento_catalogo_id tratamientoId,
                   i.diagnostico_id diagnosticoId, i.numero_pieza numeroPieza,
                   i.descripcion, i.precio, i.orden_fase ordenFase, i.estado, i.atencion_id atencionId,
                   t.nombre nombreTratamiento, t.categoria categoriaTratamiento,
                   d.descripcion diagnosticoDescripcion
            FROM clinica_plan_tratamiento_items i
            LEFT JOIN clinica_catalogo_tratamientos t ON t.id = i.tratamiento_catalogo_id
            LEFT JOIN clinica_paciente_diagnosticos d ON d.id = i.diagnostico_id
            WHERE i.plan_id = ?
            ORDER BY i.orden_fase ASC, i.id ASC
        """;
        return consultar(sql, planId);
    }

    public long insertarPlan(long pacienteId, String titulo, String estado, BigDecimal total, String notas, String actor) {
        String sql = """
            INSERT INTO clinica_planes_tratamiento (paciente_id, titulo, estado, costo_total, notas, creado_por)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        return insertar(sql, pacienteId, titulo, estado, total, notas, actor);
    }

    public void actualizarPlan(long planId, String titulo, String estado, BigDecimal total, String notas) {
        actualizar("UPDATE clinica_planes_tratamiento SET titulo = ?, estado = ?, costo_total = ?, notas = ? WHERE id = ?",
                titulo, estado, total, notas, planId);
    }

    public void insertarItemPlan(long planId, Long tratamientoId, Long diagnosticoId, Integer numeroPieza,
                                 String descripcion, BigDecimal precio, int fase, String estado) {
        String sql = """
            INSERT INTO clinica_plan_tratamiento_items (plan_id, tratamiento_catalogo_id, diagnostico_id, numero_pieza, descripcion, precio, orden_fase, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        insertar(sql, planId, tratamientoId, diagnosticoId, numeroPieza, descripcion, precio, fase, estado);
    }

    public void limpiarItemsPlan(long planId) {
        actualizar("DELETE FROM clinica_plan_tratamiento_items WHERE plan_id = ?", planId);
    }

    public void actualizarEstadoPlan(long planId, String estado) {
        actualizar("UPDATE clinica_planes_tratamiento SET estado = ? WHERE id = ?", estado, planId);
    }

    public void actualizarEstadoItemPlan(long itemId, String estado, Long atencionId) {
        actualizar("UPDATE clinica_plan_tratamiento_items SET estado = ?, atencion_id = COALESCE(?, atencion_id) WHERE id = ?",
                estado, atencionId, itemId);
    }

    public void vincularPresupuestoPlan(long planId, long presupuestoId) {
        actualizar("UPDATE clinica_planes_tratamiento SET presupuesto_id = ?, estado = 'APROBADO' WHERE id = ?",
                presupuestoId, planId);
    }

    // --- ATENCIONES / EVOLUCIONES SOEP ---
    public List<Map<String, Object>> listarAtencionesPaciente(long pacienteId) {
        String sql = """
            SELECT a.id, a.paciente_id pacienteId, a.profesional_id profesionalId, a.cita_id citaId,
                   a.fecha_atencion fechaAtencion, a.motivo_consulta motivoConsulta,
                   a.subjetivo, a.objetivo, a.evaluacion, a.plan_procedimiento planProcedimiento,
                   a.piezas_tratadas piezasTratadas, a.anestesia_utilizada anestesia,
                   a.materiales, a.indicaciones_receta receta, a.proxima_cita_sugerida proximaCita,
                   a.creado_por creadoPor, a.creado_en creadoEn,
                   CONCAT(prof.nombres, ' ', prof.apellidos) nombreProfesional,
                   c.fecha_hora_inicio citaFecha
            FROM clinica_atenciones a
            LEFT JOIN profesionales prof ON prof.id_profesional = a.profesional_id
            LEFT JOIN citas c ON c.id_cita = a.cita_id
            WHERE a.paciente_id = ?
            ORDER BY a.fecha_atencion DESC, a.id DESC
        """;
        return consultar(sql, pacienteId);
    }

    public Map<String, Object> obtenerAtencion(long atencionId) {
        String sql = """
            SELECT a.*, CONCAT(prof.nombres, ' ', prof.apellidos) nombreProfesional,
                   CONCAT(p.nombres, ' ', p.apellidos) nombrePaciente, p.dni
            FROM clinica_atenciones a
            JOIN pacientes p ON p.id_paciente = a.paciente_id
            LEFT JOIN profesionales prof ON prof.id_profesional = a.profesional_id
            WHERE a.id = ?
        """;
        return unoOpcional(sql, atencionId);
    }

    public long insertarAtencion(long pacienteId, Long profesionalId, Long citaId, String fechaHora,
                                 String motivo, String subjetivo, String objetivo, String evaluacion,
                                 String planProcedimiento, String piezasTratadas, String anestesia,
                                 String materiales, String receta, String proximaCita, String actor) {
        String sql = """
            INSERT INTO clinica_atenciones (
                paciente_id, profesional_id, cita_id, fecha_atencion, motivo_consulta,
                subjetivo, objetivo, evaluacion, plan_procedimiento, piezas_tratadas,
                anestesia_utilizada, materiales, indicaciones_receta, proxima_cita_sugerida, creado_por
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        return insertar(sql, pacienteId, profesionalId, citaId, fechaHora, motivo,
                subjetivo, objetivo, evaluacion, planProcedimiento, piezasTratadas,
                anestesia, materiales, receta, proximaCita, actor);
    }

    // --- LISTAS DE APOYO ---
    public List<Map<String, Object>> listarProfesionalesActivos() {
        return consultar("SELECT id_profesional id, CONCAT(nombres, ' ', apellidos) nombre FROM profesionales WHERE activo = 1 ORDER BY apellidos, nombres");
    }

    public List<Map<String, Object>> listarCitasPaciente(long pacienteId) {
        String sql = """
            SELECT c.id_cita id, c.fecha_hora_inicio fecha, c.estado, c.motivo_consulta motivo,
                   c.id_profesional profesionalId,
                   CONCAT(prof.nombres, ' ', prof.apellidos) profesional,
                   COALESCE((
                       SELECT e.nombre
                       FROM profesional_especialidades pe
                       JOIN especialidades e ON e.id_especialidad = pe.id_especialidad
                       WHERE pe.id_profesional = prof.id_profesional
                       LIMIT 1
                   ), 'Odontología General') especialidad,
                   ta.nombre tipoAtencion
            FROM citas c
            LEFT JOIN profesionales prof ON prof.id_profesional = c.id_profesional
            LEFT JOIN tipos_atencion ta ON ta.id_tipo_atencion = c.id_tipo_atencion
            WHERE c.id_paciente = ?
            ORDER BY c.fecha_hora_inicio DESC LIMIT 50
        """;
        return consultar(sql, pacienteId);
    }

    public List<Map<String, Object>> listarPresupuestosPaciente(long pacienteId) {
        String sql = """
            SELECT b.id, b.titulo, b.fecha, b.vence, b.estado, b.total, b.notas,
                   COALESCE((SELECT SUM(p.monto) FROM gestion_pagos p WHERE p.presupuesto_id = b.id AND p.estado = 'REGISTRADO'), 0) pagado,
                   (b.total - COALESCE((SELECT SUM(p.monto) FROM gestion_pagos p WHERE p.presupuesto_id = b.id AND p.estado = 'REGISTRADO'), 0)) saldo
            FROM gestion_presupuestos b
            WHERE b.paciente_id = ?
            ORDER BY b.id DESC
        """;
        return consultar(sql, pacienteId);
    }

    public List<Map<String, Object>> listarPagosPaciente(long pacienteId) {
        String sql = """
            SELECT p.id, p.presupuesto_id presupuestoId, p.monto, p.fecha, p.metodo, p.referencia, p.estado,
                   p.creado_por creadoPor, p.creado_en creadoEn, b.titulo presupuestoTitulo
            FROM gestion_pagos p
            JOIN gestion_presupuestos b ON b.id = p.presupuesto_id
            WHERE b.paciente_id = ?
            ORDER BY p.fecha DESC, p.id DESC
        """;
        return consultar(sql, pacienteId);
    }
}
