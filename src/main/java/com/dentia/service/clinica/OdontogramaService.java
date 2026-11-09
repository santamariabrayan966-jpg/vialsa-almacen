package com.dentia.service.clinica;

import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.dto.clinica.ClinicaRequests.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Reglas FDI, escritura serializada e historial del odontograma. */
@Service
public class OdontogramaService {
    private static final Set<String> CONDICIONES = Set.of("SANO", "CARIES", "OBTURACION", "SELLANTE", "CORONA", "ENDODONCIA", "BRACKETS", "AUSENTE", "IMPLANTE", "RETENIDO", "FRACTURA");
    private static final Set<String> SUPERFICIES = Set.of("GENERAL", "MESIAL", "DISTAL", "VESTIBULAR", "PALATINO", "LINGUAL", "OCLUSAL", "INCISAL");
    private static final Set<String> ESTADOS = Set.of("PATOLOGICO", "REALIZADO", "TEMPORAL", "SANO");
    private final JdbcClinicaDao dao;
    public OdontogramaService(JdbcClinicaDao dao) { this.dao = dao; }

    public static void validarPieza(Integer numero) {
        if (numero == null) throw new IllegalArgumentException("Selecciona una pieza dental.");
        int cuadrante = numero / 10, pieza = numero % 10;
        if (!(cuadrante >= 1 && cuadrante <= 4 && pieza >= 1 && pieza <= 8)
                && !(cuadrante >= 5 && cuadrante <= 8 && pieza >= 1 && pieza <= 5)) {
            throw new IllegalArgumentException("Número FDI inválido: " + numero);
        }
    }

    private static String normalizar(String valor, String defecto, Set<String> valores, String nombre) {
        String texto = valor == null ? defecto : valor.trim().toUpperCase(Locale.ROOT);
        if (!valores.contains(texto)) throw new IllegalArgumentException(nombre + " no válido.");
        return texto;
    }

    private long bloquear(Long id, Long version) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Selecciona un odontograma.");
        if (dao.unoOpcional("SELECT id FROM clinica_odontogramas WHERE id = ? FOR UPDATE", id) == null)
            throw new NoSuchElementException("Odontograma no encontrado.");
        dao.actualizar("INSERT IGNORE INTO clinica_odontograma_versiones (odontograma_id) VALUES (?)", id);
        var row = dao.unoOpcional("SELECT version FROM clinica_odontograma_versiones WHERE odontograma_id = ?", id);
        long actual = ((Number) row.get("version")).longValue();
        if (version != null && version != actual) throw new EdicionConcurrenteException();
        return actual;
    }

    private void historial(long id, int pieza, String superficie, String accion, String condicion, String notas, String actor) {
        dao.actualizar("INSERT INTO clinica_odontograma_historial (odontograma_id, numero_pieza, superficie, accion, condicion, notas, usuario) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, pieza, superficie, accion, condicion, notas, actor);
    }

    private void incrementar(long id) { dao.actualizar("UPDATE clinica_odontograma_versiones SET version = version + 1 WHERE odontograma_id = ?", id); }

    @Transactional
    public void registrar(OdontogramaPiezaInput input, String actor) {
        if (input == null) throw new IllegalArgumentException("Faltan datos del hallazgo.");
        validarPieza(input.numeroPieza());
        String condicion = normalizar(input.condicion(), "SANO", CONDICIONES, "Condición");
        String superficie = normalizar(input.superficie(), "GENERAL", SUPERFICIES, "Superficie");
        String estado = normalizar(input.estado(), "PATOLOGICO", ESTADOS, "Estado");
        String notas = input.notas() == null ? null : input.notas().trim();
        if (notas != null && notas.length() > 255) throw new IllegalArgumentException("La nota admite hasta 255 caracteres.");
        bloquear(input.odontogramaId(), input.version());
        dao.guardarPiezaOdontograma(input.odontogramaId(), input.numeroPieza(), superficie, condicion, estado, notas);
        historial(input.odontogramaId(), input.numeroPieza(), superficie, "HALLAZGO", condicion, notas, actor);
        incrementar(input.odontogramaId());
    }

    @Transactional
    public void limpiar(LimpiarPiezaInput input, String actor) {
        validarPieza(input.numeroPieza()); bloquear(input.odontogramaId(), input.version());
        dao.limpiarPiezaOdontograma(input.odontogramaId(), input.numeroPieza());
        historial(input.odontogramaId(), input.numeroPieza(), "GENERAL", "LIMPIAR", null, "Hallazgos retirados del estado actual", actor);
        incrementar(input.odontogramaId());
    }

    @Transactional
    public void anotar(NotaPiezaInput input, String actor) {
        if (input == null) throw new IllegalArgumentException("Faltan los datos de la nota.");
        validarPieza(input.numeroPieza());
        String nota = input.nota() == null ? "" : input.nota().trim();
        if (nota.isBlank() || nota.length() > 255)
            throw new IllegalArgumentException("Escribe una nota de hasta 255 caracteres.");
        bloquear(input.odontogramaId(), input.version());
        historial(input.odontogramaId(), input.numeroPieza(), "GENERAL", "NOTA", null, nota, actor);
        incrementar(input.odontogramaId());
    }

    @Transactional
    public void configurar(ConfiguracionOdontogramaInput input, String actor) {
        if (input == null || input.piezas() == null || input.piezas().isEmpty() || input.piezas().size() > 32)
            throw new IllegalArgumentException("Selecciona entre 1 y 32 piezas para ajustar.");
        Set<Integer> ids = new HashSet<>();
        for (PosicionPiezaInput pieza : input.piezas()) {
            if (pieza == null) throw new IllegalArgumentException("Pieza vacía.");
            validarPieza(pieza.numeroPieza());
            if (pieza.numeroPieza() >= 50 || !ids.add(pieza.numeroPieza())) throw new IllegalArgumentException("La geometría 3D admite piezas permanentes sin duplicados.");
            if (pieza.inclinacion() == null || !Double.isFinite(pieza.inclinacion()) || Math.abs(pieza.inclinacion()) > 90)
                throw new IllegalArgumentException("La inclinación debe estar entre -90° y 90°.");
            if (pieza.presencia() != null) normalizar(pieza.presencia(), "PRESENTE", Set.of("PRESENTE", "AUSENTE", "RETENIDO"), "Presencia");
        }
        bloquear(input.odontogramaId(), input.version());
        for (PosicionPiezaInput pieza : input.piezas()) {
            dao.actualizar("""
                INSERT INTO clinica_odontograma_posiciones (odontograma_id, numero_pieza, inclinacion, actualizado_por)
                VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE inclinacion=VALUES(inclinacion), actualizado_por=VALUES(actualizado_por)
                """, input.odontogramaId(), pieza.numeroPieza(), pieza.inclinacion(), actor);
            if (pieza.presencia() != null) {
                String presencia = pieza.presencia().trim().toUpperCase(Locale.ROOT);
                // Presencia only replaces presence findings; crowns/restorations remain intact.
                dao.actualizar("DELETE FROM clinica_odontograma_piezas WHERE odontograma_id=? AND numero_pieza=? AND superficie='GENERAL' AND condicion IN ('AUSENTE','RETENIDO')",
                        input.odontogramaId(), pieza.numeroPieza());
                if (!presencia.equals("PRESENTE")) dao.actualizar("INSERT INTO clinica_odontograma_piezas (odontograma_id,numero_pieza,superficie,condicion,estado,notas) VALUES (?,?,'GENERAL',?,'PATOLOGICO','Registrado manualmente en 3D')",
                        input.odontogramaId(), pieza.numeroPieza(), presencia);
            }
            historial(input.odontogramaId(), pieza.numeroPieza(), "GENERAL", "AJUSTE_3D", pieza.presencia(), "Inclinación ilustrativa: " + pieza.inclinacion() + "°", actor);
        }
        incrementar(input.odontogramaId());
    }

    public static class EdicionConcurrenteException extends RuntimeException {
        public EdicionConcurrenteException() { super("Otro profesional actualizó este registro. Recarga el expediente antes de guardar."); }
    }
}
