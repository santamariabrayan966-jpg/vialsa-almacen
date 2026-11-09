package com.dentia.service.clinica;

import com.dentia.service.agenda.AgendaValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/** El catálogo clínico es la fuente única de servicios, precios y tiempos orientativos. */
@Service
public class ServicioDentalCatalogoService {
    private final JdbcTemplate jdbc;

    public ServicioDentalCatalogoService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record ServicioInput(Long id, String codigo, String nombre, String categoria,
                                String subcategoria, String descripcion, BigDecimal precio, Integer duracion) {}

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listar() {
        return jdbc.queryForList("""
            SELECT t.id, t.codigo, t.nombre, t.categoria, t.descripcion,
                   COALESCE(d.subcategoria, 'Otros procedimientos') subcategoria,
                   t.precio_sugerido precio, t.duracion_estimada_min duracion
            FROM clinica_catalogo_tratamientos t
            LEFT JOIN clinica_servicio_detalles d ON d.tratamiento_id = t.id
            WHERE t.activo = 1
            ORDER BY t.categoria, subcategoria, t.nombre
            """);
    }

    @Transactional(readOnly = true)
    public void validarActivo(long servicioId) {
        Integer encontrados = jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinica_catalogo_tratamientos WHERE id=? AND activo=1",
                Integer.class, servicioId);
        if (encontrados == null || encontrados == 0)
            throw new AgendaValidationException("El servicio seleccionado no está disponible.");
    }

    @Transactional
    public long guardar(ServicioInput input) {
        if (input == null) throw new IllegalArgumentException("Faltan datos del servicio.");
        String categoria = texto(input.categoria(), 80, "Categoría").toUpperCase().trim();
        String subcategoria = input.subcategoria() == null || input.subcategoria().isBlank()
                ? "General"
                : input.subcategoria().trim();
        if (subcategoria.length() > 80) throw new IllegalArgumentException("La subcategoría admite hasta 80 caracteres.");
        String nombre = texto(input.nombre(), 120, "Nombre");
        String descripcion = input.descripcion() == null ? "" : input.descripcion().trim();
        if (descripcion.length() > 500) throw new IllegalArgumentException("La descripción admite 500 caracteres.");
        if (input.precio() == null || input.precio().signum() < 0 || input.precio().scale() > 2)
            throw new IllegalArgumentException("Indica un precio válido de hasta dos decimales; usa 0 si falta confirmar.");
        if (input.duracion() == null || input.duracion() < 5 || input.duracion() > 480)
            throw new IllegalArgumentException("El tiempo estimado debe estar entre 5 y 480 minutos.");

        String codigo;
        if (input.id() != null) {
            Map<String, Object> actual = jdbc.queryForMap(
                    "SELECT codigo, nombre, categoria FROM clinica_catalogo_tratamientos WHERE id = ?", input.id());
            String catActual = (String) actual.get("categoria");
            String codigoActual = (String) actual.get("codigo");

            String codigoFinal;
            if (input.codigo() != null && !input.codigo().isBlank() && !input.codigo().trim().equalsIgnoreCase(codigoActual)) {
                codigoFinal = texto(input.codigo(), 30, "Código").toUpperCase().trim();
                Integer existe = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM clinica_catalogo_tratamientos WHERE codigo = ? AND id != ?",
                        Integer.class, codigoFinal, input.id());
                if (existe != null && existe > 0) {
                    codigoFinal = generarCodigoSugerido(categoria);
                }
            } else if (!categoria.equalsIgnoreCase(catActual)) {
                codigoFinal = generarCodigoSugerido(categoria);
            } else {
                codigoFinal = codigoActual;
            }

            jdbc.update("""
                    UPDATE clinica_catalogo_tratamientos
                    SET codigo=?, nombre=?, categoria=?, descripcion=?, precio_sugerido=?, duracion_estimada_min=?
                    WHERE id=?
                    """, codigoFinal, nombre, categoria, descripcion, input.precio(), input.duracion(), input.id());
            jdbc.update("""
                    INSERT INTO clinica_servicio_detalles (tratamiento_id, subcategoria) VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE subcategoria=VALUES(subcategoria)
                    """, input.id(), subcategoria);
            jdbc.update("""
                    UPDATE tipos_atencion ta JOIN clinica_servicio_detalles d ON d.tipo_atencion_id=ta.id_tipo_atencion
                    SET ta.nombre=?, ta.duracion_minutos=?, ta.descripcion=? WHERE d.tratamiento_id=?
                    """, nombre, input.duracion(), descripcion, input.id());
            return input.id();
        }

        if (input.codigo() == null || input.codigo().isBlank() || "AUTO".equalsIgnoreCase(input.codigo().trim())) {
            codigo = generarCodigoSugerido(categoria);
        } else {
            codigo = texto(input.codigo(), 30, "Código").toUpperCase().trim();
        }

        GeneratedKeyHolder key = new GeneratedKeyHolder();
        String codigoFinal = codigo;
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO clinica_catalogo_tratamientos
                    (codigo, nombre, categoria, descripcion, precio_sugerido, duracion_estimada_min, activo)
                VALUES (?, ?, ?, ?, ?, ?, 1)
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, codigoFinal); ps.setString(2, nombre); ps.setString(3, categoria);
            ps.setString(4, descripcion); ps.setBigDecimal(5, input.precio()); ps.setInt(6, input.duracion());
            return ps;
        }, key);
        long id = key.getKey().longValue();
        jdbc.update("INSERT INTO clinica_servicio_detalles (tratamiento_id, subcategoria) VALUES (?, ?)", id, subcategoria);
        return id;
    }

    /** Genera un código automático secuencial no repetido para la categoría indicada. */
    @Transactional
    public String generarCodigoSugerido(String categoria) {
        String prefijo = obtenerPrefijoCategoria(categoria);
        List<String> codigos = jdbc.queryForList(
                "SELECT codigo FROM clinica_catalogo_tratamientos WHERE codigo LIKE ?",
                String.class, prefijo + "-%");
        int max = 0;
        for (String c : codigos) {
            try {
                String[] partes = c.split("-");
                if (partes.length >= 2) {
                    int num = Integer.parseInt(partes[partes.length - 1]);
                    if (num > max) max = num;
                }
            } catch (NumberFormatException ignored) {}
        }
        int sig = max + 1;
        String cand = String.format("%s-%02d", prefijo, sig);
        while (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM clinica_catalogo_tratamientos WHERE codigo = ?", Boolean.class, cand))) {
            sig++;
            cand = String.format("%s-%02d", prefijo, sig);
        }
        return cand;
    }

    public static String obtenerPrefijoCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return "VZ-SRV";
        String cat = categoria.toUpperCase().trim();
        return switch (cat) {
            case "CIRUGIA", "CIRUGIAS" -> "VZ-CI";
            case "ENDODONCIA" -> "VZ-EN";
            case "ORTODONCIA" -> "VZ-OR";
            case "PREVENTIVA", "HIGIENE", "LIMPIEZA" -> "VZ-PR";
            case "REHABILITACION", "PROTESIS" -> "VZ-RH";
            case "OPERATORIA", "RESTAURACION", "RESTAURACIONES" -> "VZ-RE";
            case "ESTETICA" -> "VZ-ES";
            case "PERIODONCIA" -> "VZ-PE";
            case "EVALUACION", "CONSULTA" -> "VZ-CO";
            case "ODONTOPEDIATRIA", "PEDIATRIA" -> "VZ-PD";
            case "IMPLANTOLOGIA", "IMPLANTES" -> "VZ-IM";
            default -> {
                String limpia = cat.replaceAll("[^A-Z0-9]", "");
                yield "VZ-" + (limpia.length() >= 2 ? limpia.substring(0, Math.min(3, limpia.length())) : "SRV");
            }
        };
    }

    /** Crea el identificador técnico de agenda solo al reservar; no duplica la lista visible. */
    @Transactional
    public long tipoAgendaParaServicio(long servicioId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT t.nombre, t.descripcion, t.duracion_estimada_min duracion, d.tipo_atencion_id tipoId
            FROM clinica_catalogo_tratamientos t
            LEFT JOIN clinica_servicio_detalles d ON d.tratamiento_id=t.id
            WHERE t.id=? AND t.activo=1 FOR UPDATE
            """, servicioId);
        if (rows.isEmpty()) throw new AgendaValidationException("El servicio seleccionado no está disponible.");
        Map<String, Object> row = rows.get(0);
        if (row.get("tipoId") != null) {
            long vinculado = ((Number) row.get("tipoId")).longValue();
            jdbc.update("UPDATE tipos_atencion SET activo=1, duracion_minutos=? WHERE id_tipo_atencion=?",
                    row.get("duracion"), vinculado);
            return vinculado;
        }
        String nombre = (String) row.get("nombre");
        if (nombre.length() > 120) throw new AgendaValidationException("El nombre del servicio supera el límite de la agenda.");
        List<Long> existentes = jdbc.query("SELECT id_tipo_atencion FROM tipos_atencion WHERE nombre=?",
                (rs, index) -> rs.getLong(1), nombre);
        long tipoId;
        if (!existentes.isEmpty()) {
            tipoId = existentes.get(0);
            jdbc.update("UPDATE tipos_atencion SET activo=1, duracion_minutos=? WHERE id_tipo_atencion=?",
                    row.get("duracion"), tipoId);
        } else {
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO tipos_atencion (nombre, descripcion, duracion_minutos, activo)
                    VALUES (?, ?, ?, 1)
                    """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, nombre);
                ps.setString(2, (String) row.get("descripcion"));
                ps.setInt(3, ((Number) row.get("duracion")).intValue());
                return ps;
            }, key);
            tipoId = key.getKey().longValue();
        }
        jdbc.update("""
            INSERT INTO clinica_servicio_detalles (tratamiento_id, subcategoria, tipo_atencion_id)
            VALUES (?, 'Otros procedimientos', ?)
            ON DUPLICATE KEY UPDATE tipo_atencion_id=VALUES(tipo_atencion_id)
            """, servicioId, tipoId);
        return tipoId;
    }

    /** Obtiene o genera el identificador técnico estándar de agenda (Consulta general) para citas sin servicio tarifado. */
    @Transactional
    public long tipoAgendaPorDefecto() {
        List<Long> existentes = jdbc.query(
                "SELECT id_tipo_atencion FROM tipos_atencion WHERE activo = 1 ORDER BY CASE WHEN nombre LIKE '%Consulta%' THEN 1 ELSE 2 END, id_tipo_atencion LIMIT 1",
                (rs, index) -> rs.getLong(1));
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO tipos_atencion (nombre, descripcion, duracion_minutos, activo)
                VALUES ('Consulta general', 'Consulta odontológica general', 30, 1)
                """, Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarInactivos() {
        return jdbc.queryForList("""
            SELECT t.id, t.codigo, t.nombre, t.categoria, t.descripcion,
                   COALESCE(d.subcategoria, 'Otros procedimientos') subcategoria,
                   t.precio_sugerido precio, t.duracion_estimada_min duracion,
                   t.activo
            FROM clinica_catalogo_tratamientos t
            LEFT JOIN clinica_servicio_detalles d ON d.tratamiento_id = t.id
            WHERE t.activo = 0
            ORDER BY t.categoria, subcategoria, t.nombre
            """);
    }

    @Transactional
    public String eliminarOSuspender(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.id, t.nombre, d.tipo_atencion_id FROM clinica_catalogo_tratamientos t " +
                "LEFT JOIN clinica_servicio_detalles d ON d.tratamiento_id = t.id WHERE t.id = ?", id);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("El servicio no existe o ya fue retirado.");
        }
        Map<String, Object> row = rows.get(0);
        String nombre = (String) row.get("nombre");
        Long tipoAtencionId = row.get("tipo_atencion_id") != null ? ((Number) row.get("tipo_atencion_id")).longValue() : null;

        // Verificar uso en planes de tratamiento o presupuestos
        Integer usoPlanes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM clinica_plan_tratamiento_items WHERE tratamiento_catalogo_id = ?",
                Integer.class, id);
        int totalPlanes = usoPlanes != null ? usoPlanes : 0;

        // Verificar uso en citas
        int totalCitas = 0;
        if (tipoAtencionId != null) {
            Integer usoCitas = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM citas WHERE id_tipo_atencion = ?",
                    Integer.class, tipoAtencionId);
            totalCitas = usoCitas != null ? usoCitas : 0;
        }

        if (totalPlanes > 0 || totalCitas > 0) {
            // Protección de datos clínicos: Desactivar (borrado lógico)
            jdbc.update("UPDATE clinica_catalogo_tratamientos SET activo = 0 WHERE id = ?", id);
            if (tipoAtencionId != null) {
                jdbc.update("UPDATE tipos_atencion SET activo = 0 WHERE id_tipo_atencion = ?", tipoAtencionId);
            }
            return "El servicio '" + nombre + "' fue desactivado del catálogo para proteger el historial médico (" +
                   (totalPlanes + totalCitas) + " registros asociados). Puedes reactivarlo en cualquier momento.";
        } else {
            // Sin historial: Eliminación física limpia
            jdbc.update("DELETE FROM clinica_servicio_detalles WHERE tratamiento_id = ?", id);
            jdbc.update("DELETE FROM clinica_catalogo_tratamientos WHERE id = ?", id);
            if (tipoAtencionId != null) {
                jdbc.update("DELETE FROM tipos_atencion WHERE id_tipo_atencion = ?", tipoAtencionId);
            }
            return "El servicio '" + nombre + "' fue eliminado definitivamente del catálogo.";
        }
    }

    @Transactional
    public void reactivar(long id) {
        jdbc.update("UPDATE clinica_catalogo_tratamientos SET activo = 1 WHERE id = ?", id);
        List<Long> tipoIds = jdbc.query(
                "SELECT tipo_atencion_id FROM clinica_servicio_detalles WHERE tratamiento_id = ?",
                (rs, i) -> rs.getLong(1));
        if (!tipoIds.isEmpty() && tipoIds.get(0) != null && tipoIds.get(0) > 0) {
            jdbc.update("UPDATE tipos_atencion SET activo = 1 WHERE id_tipo_atencion = ?", tipoIds.get(0));
        }
    }

    private static String texto(String value, int max, String campo) {
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new IllegalArgumentException(campo + " es obligatorio y admite hasta " + max + " caracteres.");
        return value.trim();
    }
}
