package com.dentia.service.interno;

import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.dto.clinica.ClinicaRequests.*;
import org.springframework.stereotype.Service;
import com.dentia.service.clinica.OdontogramaService;
import com.dentia.service.clinica.AtencionClinicaService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ClinicaInternaService {

    public static final List<String> MODULOS_CLINICOS = List.of(
            "odontograma",
            "historia-clinica",
            "diagnosticos",
            "plan-tratamiento",
            "atenciones",
            "cuentas-pagos",
            "historial-citas"
    );

    private final JdbcClinicaDao dao;

    private final OdontogramaService odontogramas;
    private final AtencionClinicaService atenciones;

    public ClinicaInternaService(JdbcClinicaDao dao, OdontogramaService odontogramas, AtencionClinicaService atenciones) {
        this.dao = dao;
        this.odontogramas = odontogramas;
        this.atenciones = atenciones;
    }

    // --- CARGA INTEGRAL DEL PACIENTE ---
    @Transactional
    public Map<String, Object> cargarExpedienteCompleto(long pacienteId) {
        // Serialize first creation of the initial odontogram for this patient.
        dao.unoOpcional("SELECT id_paciente FROM pacientes WHERE id_paciente = ? FOR UPDATE", pacienteId);
        var paciente = dao.obtenerPaciente(pacienteId);
        if (paciente == null) {
            throw new NoSuchElementException("El paciente solicitado no existe.");
        }

        var historia = dao.obtenerHistoria(pacienteId);
        var odontoInicial = dao.obtenerOdontogramaActivo(pacienteId, "INICIAL");
        long odontoId = ((Number) odontoInicial.get("id")).longValue();
        var piezasOdonto = dao.listarPiezasOdontograma(odontoId);

        var diagnosticos = dao.listarDiagnosticosPaciente(pacienteId, null);
        var planes = dao.listarPlanesPaciente(pacienteId);
        for (var plan : planes) {
            long planId = ((Number) plan.get("id")).longValue();
            plan.put("items", dao.listarItemsPlan(planId));
        }
        var atenciones = dao.listarAtencionesPaciente(pacienteId);
        var citas = dao.listarCitasPaciente(pacienteId);

        // Finanzas: Presupuestos, Pagos y Deudas
        var presupuestos = dao.listarPresupuestosPaciente(pacienteId);
        var pagos = dao.listarPagosPaciente(pacienteId);
        BigDecimal totalPresupuestado = BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        for (var pr : presupuestos) {
            if ("APROBADO".equals(pr.get("estado"))) {
                totalPresupuestado = totalPresupuestado.add(new BigDecimal(String.valueOf(pr.get("total"))));
            }
        }
        for (var pg : pagos) {
            if ("REGISTRADO".equals(pg.get("estado"))) {
                totalPagado = totalPagado.add(new BigDecimal(String.valueOf(pg.get("monto"))));
            }
        }
        BigDecimal saldoDeudor = totalPresupuestado.subtract(totalPagado);
        if (saldoDeudor.compareTo(BigDecimal.ZERO) < 0) saldoDeudor = BigDecimal.ZERO;

        Map<String, Object> finanzas = new LinkedHashMap<>();
        finanzas.put("totalPresupuestado", totalPresupuestado);
        finanzas.put("totalPagado", totalPagado);
        finanzas.put("saldoDeudor", saldoDeudor);
        finanzas.put("presupuestos", presupuestos);
        finanzas.put("pagos", pagos);

        // Progress comes from recorded procedures, not estimated orthodontic dates.
        Map<String, Object> ortodoncia = new LinkedHashMap<>();
        var activo = planes.stream().filter(p -> Set.of("APROBADO", "EN_EJECUCION").contains(String.valueOf(p.get("estado")))).findFirst();
        ortodoncia.put("activo", activo.isPresent());
        activo.ifPresent(plan -> {
            int total = ((Number) plan.getOrDefault("totalItems", 0)).intValue();
            int realizados = ((Number) plan.getOrDefault("itemsRealizados", 0)).intValue();
            ortodoncia.put("titulo", plan.get("titulo"));
            ortodoncia.put("totalItems", total);
            ortodoncia.put("itemsRealizados", realizados);
            ortodoncia.put("porcentajeAvance", total == 0 ? 0 : Math.round(100f * realizados / total));
            ortodoncia.put("faseActual", plan.get("estado"));
            ortodoncia.put("proximoControl", "Consultar agenda");
        });

        // Alertas médicas críticas destacadas
        List<String> alertas = new ArrayList<>();
        if (historia != null) {
            Object al = historia.get("alergias");
            if (al != null && !al.toString().isBlank()) alertas.add("⚠️ Alergia: " + al.toString().trim());
            Object enf = historia.get("enfermedades_sistemicas");
            if (enf != null && !enf.toString().isBlank()) alertas.add("🏥 " + enf.toString().trim());
            if (Boolean.TRUE.equals(historia.get("hemorragias")) || "1".equals(String.valueOf(historia.get("hemorragias")))) {
                alertas.add("🩸 Riesgo hemorrágico");
            }
            if (Boolean.TRUE.equals(historia.get("embarazo")) || "1".equals(String.valueOf(historia.get("embarazo")))) {
                alertas.add("🤰 Paciente gestante");
            }
            Object pres = historia.get("presion_arterial");
            if (pres != null && !pres.toString().isBlank()) alertas.add("P/A: " + pres.toString().trim());
        }

        Map<String, Object> expediente = new LinkedHashMap<>();
        expediente.put("paciente", paciente);
        expediente.put("alertas", alertas);
        expediente.put("historia", historia);
        expediente.put("odontograma", odontoInicial);
        expediente.put("piezasOdontograma", piezasOdonto);
        expediente.put("posicionesOdontograma", dao.consultar("SELECT numero_pieza numeroPieza, inclinacion FROM clinica_odontograma_posiciones WHERE odontograma_id = ?", odontoId));
        var revision = dao.unoOpcional("SELECT version FROM clinica_odontograma_versiones WHERE odontograma_id = ?", odontoId);
        expediente.put("versionOdontograma", revision == null ? 0 : revision.get("version"));
        expediente.put("historialOdontograma", dao.consultar("SELECT numero_pieza numeroPieza, superficie, accion, condicion, notas, usuario, fecha FROM clinica_odontograma_historial WHERE odontograma_id = ? ORDER BY id DESC LIMIT 300", odontoId));
        expediente.put("borradorAtencion", obtenerBorrador(pacienteId));
        expediente.put("diagnosticos", diagnosticos);
        expediente.put("planes", planes);
        expediente.put("atenciones", atenciones);
        expediente.put("citas", citas);
        expediente.put("finanzas", finanzas);
        expediente.put("ortodoncia", ortodoncia);
        return expediente;
    }

    // --- HISTORIA CLÍNICA ---
    @Transactional
    public void guardarHistoria(HistoriaInput input, String actor) {
        Objects.requireNonNull(input, "Faltan datos de la historia.");
        if (input.pacienteId() == null) throw new IllegalArgumentException("Se requiere el paciente.");
        dao.guardarHistoria(
                input.pacienteId(),
                limpiar(input.alergias(), 500),
                limpiar(input.enfermedadesSistemicas(), 500),
                limpiar(input.medicacionActual(), 500),
                Boolean.TRUE.equals(input.hemorragias()),
                Boolean.TRUE.equals(input.embarazo()),
                limpiar(input.antecedentesFamiliares(), 500),
                limpiar(input.habitos(), 500),
                limpiar(input.motivoPrimeraConsulta(), 1000),
                limpiar(input.grupoSanguineo(), 10),
                limpiar(input.presionArterial(), 20),
                limpiar(input.observacionesMedicas(), 2000),
                actor != null ? actor : "SISTEMA"
        );
    }

    // --- ODONTOGRAMA ---
    public void registrarHallazgoOdontograma(OdontogramaPiezaInput input, String actor) { odontogramas.registrar(input, actor); }
    public void limpiarPiezaOdontograma(LimpiarPiezaInput input, String actor) { odontogramas.limpiar(input, actor); }
    public void anotarPiezaOdontograma(NotaPiezaInput input, String actor) { odontogramas.anotar(input, actor); }
    public void configurarOdontograma(ConfiguracionOdontogramaInput input, String actor) { odontogramas.configurar(input, actor); }

    public Map<String, Object> obtenerBorrador(long pacienteId) { return atenciones.obtenerBorrador(pacienteId); }
    public Map<String, Object> guardarBorrador(BorradorAtencionInput input, String actor) { return atenciones.guardarBorrador(input, actor); }

    // --- DIAGNÓSTICOS ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarCatalogoDiagnosticos(String categoria, String q) {
        return dao.listarCatalogoDiagnosticos(categoria, q);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDiagnosticosPaciente(long pacienteId, String estado) {
        return dao.listarDiagnosticosPaciente(pacienteId, estado);
    }

    @Transactional
    public long registrarDiagnostico(DiagnosticoInput input, String actor) {
        Objects.requireNonNull(input, "Faltan datos del diagnóstico.");
        if (input.pacienteId() == null) throw new IllegalArgumentException("Se requiere el paciente.");
        if (input.descripcion() == null || input.descripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción del diagnóstico es obligatoria.");
        }
        if (input.numeroPieza() != null) OdontogramaService.validarPieza(input.numeroPieza());
        String fecha = input.fechaDiagnostico() != null ? input.fechaDiagnostico().toString() : LocalDate.now().toString();
        return dao.insertarDiagnostico(
                input.pacienteId(),
                input.diagnosticoCatalogoId(),
                limpiar(input.codigoCie10(), 20),
                limpiar(input.descripcion(), 250),
                input.numeroPieza(),
                input.gravedad() == null ? "MODERADA" : input.gravedad(),
                input.estado() == null ? "ACTIVO" : input.estado(),
                fecha,
                limpiar(input.observaciones(), 1000),
                actor != null ? actor : "SISTEMA"
        );
    }

    @Transactional
    public void actualizarEstadoDiagnostico(long id, String nuevoEstado) {
        if (!Set.of("ACTIVO", "EN_TRATAMIENTO", "RESUELTO").contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado no válido.");
        }
        dao.actualizarEstadoDiagnostico(id, nuevoEstado);
    }

    // --- TRATAMIENTOS ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarCatalogoTratamientos(String categoria, String q) {
        return dao.listarCatalogoTratamientos(categoria, q);
    }

    @Transactional
    public long guardarTratamientoCatalogo(TratamientoInput input) {
        Objects.requireNonNull(input, "Faltan datos del tratamiento.");
        if (input.nombre() == null || input.nombre().isBlank()) throw new IllegalArgumentException("El nombre es requerido.");
        BigDecimal precio = input.precioSugerido() != null && input.precioSugerido().compareTo(BigDecimal.ZERO) >= 0
                ? input.precioSugerido() : BigDecimal.ZERO;
        int duracion = input.duracionEstimadaMin() != null && input.duracionEstimadaMin() > 0
                ? input.duracionEstimadaMin() : 30;
        long id = dao.guardarTratamientoCatalogo(
                limpiar(input.codigo(), 30),
                limpiar(input.nombre(), 200),
                limpiar(input.categoria() != null ? input.categoria() : "GENERAL", 80),
                limpiar(input.descripcion(), 500),
                precio,
                duracion
        );
        String subcategoria = limpiar(input.subcategoria(), 80);
        dao.actualizar("INSERT INTO clinica_servicio_detalles (tratamiento_id, subcategoria) VALUES (?, ?)",
                id, subcategoria == null || subcategoria.isBlank() ? "Otros procedimientos" : subcategoria);
        return id;
    }

    // --- PLAN DE TRATAMIENTO ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPlanes(long pacienteId) {
        var planes = dao.listarPlanesPaciente(pacienteId);
        for (var plan : planes) {
            long planId = ((Number) plan.get("id")).longValue();
            plan.put("items", dao.listarItemsPlan(planId));
        }
        return planes;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detallePlan(long planId) {
        var plan = dao.obtenerPlan(planId);
        if (plan == null) throw new NoSuchElementException("Plan no encontrado.");
        var items = dao.listarItemsPlan(planId);
        Map<String, Object> res = new LinkedHashMap<>(plan);
        res.put("items", items);
        return res;
    }

    @Transactional
    public long guardarPlanTratamiento(Long planId, PlanTratamientoInput input, String actor) {
        Objects.requireNonNull(input, "Faltan datos del plan.");
        if (input.pacienteId() == null) throw new IllegalArgumentException("Se requiere el paciente.");
        String titulo = (input.titulo() == null || input.titulo().isBlank()) ? "Plan de Tratamiento Integral" : input.titulo().trim();

        BigDecimal costoTotal = BigDecimal.ZERO;
        if (input.items() != null) {
            for (PlanItemInput item : input.items()) {
                if (item.precio() != null && item.precio().compareTo(BigDecimal.ZERO) > 0) {
                    costoTotal = costoTotal.add(item.precio());
                }
            }
        }

        String estado = input.estado() != null ? input.estado().trim().toUpperCase() : "PROPUESTO";
        if (planId == null) {
            planId = dao.insertarPlan(input.pacienteId(), titulo, estado, costoTotal, limpiar(input.notas(), 2000), actor != null ? actor : "SISTEMA");
        } else {
            dao.actualizarPlan(planId, titulo, estado, costoTotal, limpiar(input.notas(), 2000));
            dao.limpiarItemsPlan(planId);
        }

        if (input.items() != null) {
            for (PlanItemInput item : input.items()) {
                BigDecimal precioItem = item.precio() != null ? item.precio() : BigDecimal.ZERO;
                int fase = item.ordenFase() != null && item.ordenFase() > 0 ? item.ordenFase() : 1;
                String itemEstado = item.estado() != null ? item.estado() : "PENDIENTE";
                dao.insertarItemPlan(
                        planId,
                        item.tratamientoCatalogoId(),
                        item.diagnosticoId(),
                        item.numeroPieza(),
                        limpiar(item.descripcion(), 250),
                        precioItem,
                        fase,
                        itemEstado
                );
            }
        }
        return planId;
    }

    @Transactional
    public void cambiarEstadoPlan(long planId, String nuevoEstado) {
        if (!Set.of("BORRADOR", "PROPUESTO", "APROBADO", "EN_EJECUCION", "COMPLETADO", "CANCELADO").contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado no válido.");
        }
        dao.actualizarEstadoPlan(planId, nuevoEstado);
    }

    @Transactional
    public Map<String, Object> aprobarPlanYGenerarDeuda(long planId, String actor) {
        cambiarEstadoPlan(planId, "APROBADO");
        long presupuestoId = generarPresupuestoDesdePlan(planId, actor);
        dao.actualizar("UPDATE gestion_presupuestos SET estado='APROBADO', version=version+1 WHERE id=?", presupuestoId);
        dao.insertar("INSERT INTO gestion_historial (modulo, registro_id, accion, detalle, usuario) VALUES ('presupuestos', ?, 'APROBADO', ?, ?)",
                presupuestoId, "Aprobado por el paciente al aceptar el plan de tratamiento.", actor != null ? actor : "SISTEMA");
        var plan = dao.obtenerPlan(planId);
        BigDecimal total = plan != null && plan.get("costo_total") != null ? new BigDecimal(plan.get("costo_total").toString()) : BigDecimal.ZERO;
        return Map.of("ok", true, "planId", planId, "presupuestoId", presupuestoId, "total", total);
    }

    /**
     * Interconexión directa con el módulo comercial: genera un presupuesto en gestion_presupuestos
     * a partir del plan clínico aprobado, con sus items y costos desglosados.
     */
    @Transactional
    public long generarPresupuestoDesdePlan(long planId, String actor) {
        var plan = dao.obtenerPlanParaGenerarPresupuesto(planId);
        if (plan == null) throw new NoSuchElementException("Plan no encontrado.");

        Object presupuestoVinculado = plan.get("presupuesto_id");
        if (presupuestoVinculado instanceof Number) {
            return ((Number) presupuestoVinculado).longValue();
        }

        var items = dao.listarItemsPlan(planId);
        if (items.isEmpty()) throw new IllegalArgumentException("El plan no contiene procedimientos para presupuestar.");
        // Validar todos los precios antes de insertar cabecera o detalle.
        items.forEach(this::precioValido);

        long pacienteId = ((Number) plan.get("paciente_id")).longValue();
        String titulo = "Presupuesto: " + plan.get("titulo");
        if (titulo.length() > 180) titulo = titulo.substring(0, 180);

        BigDecimal total = plan.get("costo_total") == null
                ? BigDecimal.ZERO
                : new BigDecimal(plan.get("costo_total").toString());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            total = BigDecimal.ZERO;
            for (var item : items) {
                total = total.add(precioValido(item));
            }
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El plan debe tener un costo total mayor que cero.");
        }

        LocalDate vence = LocalDate.now().plusDays(30);

        // Inserta en gestion_presupuestos
        String sqlPresupuesto = """
            INSERT INTO gestion_presupuestos (paciente_id, titulo, fecha, vence, estado, total, notas, creado_por)
            VALUES (?, ?, CURDATE(), ?, 'BORRADOR', ?, ?, ?)
        """;
        long presupuestoId = dao.insertar(sqlPresupuesto, pacienteId, titulo, vence, total,
                "Generado automáticamente desde Plan Clínico #" + planId, actor != null ? actor : "SISTEMA");

        // Inserta items
        for (var item : items) {
            String desc = item.get("descripcion").toString();
            if (item.get("numeroPieza") != null) {
                desc = "[Pza " + item.get("numeroPieza") + "] " + desc;
            }
            if (desc.length() > 250) desc = desc.substring(0, 250);
            BigDecimal precio = precioValido(item);

            dao.insertar("INSERT INTO gestion_presupuesto_items (presupuesto_id, descripcion, cantidad, precio) VALUES (?, ?, 1, ?)",
                    presupuestoId, desc, precio);
        }

        // Inserta historial de auditoría
        dao.insertar("INSERT INTO gestion_historial (modulo, registro_id, accion, detalle, usuario) VALUES ('presupuestos', ?, 'GENERADO_PLAN', ?, ?)",
                presupuestoId, "Presupuesto derivado del Plan Clínico #" + planId + ". Total: PEN " + total,
                actor != null ? actor : "SISTEMA");

        // Vincula el plan
        dao.vincularPresupuestoPlan(planId, presupuestoId);
        return presupuestoId;
    }

    private BigDecimal precioValido(Map<String, Object> item) {
        Object raw = item.get("precio");
        if (raw == null) {
            throw new IllegalArgumentException("Cada procedimiento debe tener un precio mayor que cero.");
        }
        BigDecimal precio = new BigDecimal(raw.toString());
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cada procedimiento debe tener un precio mayor que cero.");
        }
        return precio;
    }

    // --- ATENCIONES / EVOLUCIONES SOEP ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarAtenciones(long pacienteId) {
        return dao.listarAtencionesPaciente(pacienteId);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> misAtenciones(String usuario) {
        return dao.consultar("""
            SELECT a.id, a.paciente_id pacienteId, CONCAT(pac.nombres,' ',pac.apellidos) paciente,
                   a.fecha_atencion fecha, a.plan_procedimiento procedimiento, a.piezas_tratadas piezas
            FROM clinica_atenciones a JOIN pacientes pac ON pac.id_paciente=a.paciente_id
            LEFT JOIN profesionales prof ON prof.id_profesional=a.profesional_id
            LEFT JOIN usuarios u ON u.idUsuario=prof.id_usuario
            WHERE u.NombreUsuario=? OR (a.profesional_id IS NULL AND a.creado_por=?)
            ORDER BY a.fecha_atencion DESC, a.id DESC LIMIT 200
            """, usuario, usuario);
    }

    @Transactional(readOnly = true)
    public java.util.Set<Long> misPacientes(String usuario) {
        return dao.consultar("""
            SELECT DISTINCT a.paciente_id id FROM clinica_atenciones a
            LEFT JOIN profesionales prof ON prof.id_profesional=a.profesional_id
            LEFT JOIN usuarios u ON u.idUsuario=prof.id_usuario
            WHERE u.NombreUsuario=? OR (a.profesional_id IS NULL AND a.creado_por=?)
            """, usuario, usuario).stream().map(r -> ((Number)r.get("id")).longValue()).collect(java.util.stream.Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public java.util.Set<Long> pacientesAtendidosHoy() {
        var hoy = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        return dao.consultar("SELECT DISTINCT paciente_id id FROM clinica_atenciones WHERE fecha_atencion >= ? AND fecha_atencion < ?", hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay())
                .stream().map(r -> ((Number)r.get("id")).longValue()).collect(java.util.stream.Collectors.toSet());
    }

    public long registrarAtencion(FinalizarAtencionInput input, String actor) { return atenciones.registrarAtencion(input, actor); }

    // --- LISTAS DE APOYO ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProfesionales() {
        return dao.listarProfesionalesActivos();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarPacientes(String q) {
        return dao.buscarPacientes(q);
    }

    private static String limpiar(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
