package com.dentia.service.clinica;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.dto.clinica.ClinicaRequests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtencionClinicaServiceTest {
    private JdbcClinicaDao dao;
    private AtencionClinicaService service;
    private Long version;
    private String contenido;
    private boolean pacienteExiste;
    private int inserts;
    private Map<String, Object> recibo, cita, item;
    private final String solicitudId = "a5a171ca-94b1-413a-bf1d-a21c432f9a92";

    @BeforeEach void preparar() {
        pacienteExiste = true;
        dao = mock(JdbcClinicaDao.class, invocation -> {
            Object[] a = invocation.getArguments();
            String method = invocation.getMethod().getName();
            if (method.equals("unoOpcional")) {
                String sql = (String) a[0];
                if (sql.contains("FROM pacientes")) return pacienteExiste ? Map.of("id_paciente", 7L) : null;
                if (sql.contains("FROM clinica_atencion_solicitudes")) return recibo;
                if (sql.contains("FROM clinica_atencion_borradores"))
                    return version == null ? null : new HashMap<>(Map.of("version", version, "contenido", contenido));
                if (sql.contains("FROM citas")) return cita;
                if (sql.contains("FROM clinica_plan_tratamiento_items")) return item;
            }
            if (method.equals("insertarAtencion")) { inserts++; return 41L + inserts; }
            if (method.equals("actualizar")) {
                String sql = (String) a[0];
                if (sql.contains("INSERT INTO clinica_atencion_borradores")) {
                    boolean cierre = sql.contains("'null'");
                    contenido = cierre ? "null" : (String) a[2];
                    version = ((Number) a[cierre ? 2 : 3]).longValue();
                }
                if (sql.contains("INSERT INTO clinica_atencion_solicitudes"))
                    recibo = Map.of("atencion_id", a[3], "huella", a[4]);
                return 1;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        service = new AtencionClinicaService(dao, new ObjectMapper().findAndRegisterModules());
    }

    private AtencionInput datos(String procedimiento, Long citaId, Long itemId) {
        return new AtencionInput(7L, null, citaId, null, null, "Observación", null, null,
                procedimiento, "16", null, null, null, null, itemId);
    }
    private FinalizarAtencionInput cierre(long revision, AtencionInput datos) {
        return new FinalizarAtencionInput(revision, solicitudId, datos);
    }
    private BorradorAtencionInput borrador(long revision, AtencionInput datos) {
        return new BorradorAtencionInput(7L, revision, datos);
    }
    private void conflicto(Runnable operation) {
        assertThrows(OdontogramaService.EdicionConcurrenteException.class, operation::run);
    }

    @Test void guardaYRecuperaAtencionIncompleta() {
        assertEquals(1L, service.guardarBorrador(borrador(0, datos(null, null, null)), "doctor").get("version"));
        assertEquals(1L, service.obtenerBorrador(7).get("version"));
        assertTrue(contenido.contains("Observación"));
        assertEquals(0, inserts);
    }

    @Test void rechazaGuardarUnaVersionAnteriorSinCambiarContenido() {
        service.guardarBorrador(borrador(0, datos("Primero", null, null)), "doctor");
        String original = contenido;
        conflicto(() -> service.guardarBorrador(borrador(0, datos("Otro", null, null)), "doctor"));
        assertEquals(original, contenido);
        assertEquals(1L, version);
    }

    @Test void rechazaPacienteDistintoEnBorrador() {
        assertThrows(IllegalArgumentException.class, () -> service.guardarBorrador(
                new BorradorAtencionInput(8L, 0L, datos(null, null, null)), "doctor"));
        verifyNoInteractions(dao);
    }

    @Test void finalizaConRevisionMonotonaSinEliminarFilaDeControl() {
        service.guardarBorrador(borrador(0, datos(null, null, null)), "doctor");
        assertEquals(42L, service.registrarAtencion(cierre(1, datos("Restauración", null, null)), "doctor"));
        assertEquals(2L, version);
        assertEquals("null", contenido);
        assertNotNull(recibo);
        verify(dao, never()).actualizar(startsWith("DELETE"), any());
    }

    @Test void cierreSinBorradorTambienInvalidaLaVersionCero() {
        service.registrarAtencion(cierre(0, datos("Control", null, null)), "doctor");
        conflicto(() -> service.guardarBorrador(borrador(0, datos("Obsoleto", null, null)), "doctor"));
        assertEquals(1L, version);
        assertEquals("null", contenido);
    }

    @Test void finalizacionObsoletaNoInsertaNiBorraBorrador() {
        service.guardarBorrador(borrador(0, datos("Actualizado", null, null)), "doctor");
        conflicto(() -> service.registrarAtencion(cierre(0, datos("Anterior", null, null)), "doctor"));
        assertEquals(0, inserts);
        assertTrue(contenido.contains("Actualizado"));
    }

    @Test void reintentoDevuelveLaMismaAtencionInclusoSiHayUnBorradorPosterior() {
        var request = cierre(0, datos("Control", null, null));
        long id = service.registrarAtencion(request, "doctor");
        service.guardarBorrador(borrador(1, datos("Siguiente sesión", null, null)), "doctor");
        assertEquals(id, service.registrarAtencion(request, "doctor"));
        assertEquals(1, inserts);
        assertEquals(2L, version);
        assertTrue(contenido.contains("Siguiente sesión"));
    }

    @Test void mismaClaveNoAdmiteOtroContenidoNiOtroActor() {
        var request = cierre(0, datos("Control", null, null));
        service.registrarAtencion(request, "doctor");
        conflicto(() -> service.registrarAtencion(cierre(0, datos("Otro", null, null)), "doctor"));
        conflicto(() -> service.registrarAtencion(request, "otro-profesional"));
        assertEquals(1, inserts);
    }

    @Test void requiereVersionIdentificadorYProcedimiento() {
        var datos = datos("Control", null, null);
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(new FinalizarAtencionInput(null, solicitudId, datos), "doctor"));
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(new FinalizarAtencionInput(0L, "invalido", datos), "doctor"));
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(cierre(0, datos(" ", null, null)), "doctor"));
        verifyNoInteractions(dao);
    }

    @Test void noRecortaProcedimientoNiRecetaClinicaAlFinalizar() {
        AtencionInput procedimientoLargo = datos("P".repeat(2001), null, null);
        IllegalArgumentException errorProcedimiento = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAtencion(cierre(0, procedimientoLargo), "doctor"));
        assertTrue(errorProcedimiento.getMessage().contains("Procedimiento realizado"));

        AtencionInput recetaLarga = new AtencionInput(7L, null, null, null, null, "Observación", null, null,
                "Control", "16", null, null, "R".repeat(2001), null, null);
        IllegalArgumentException errorReceta = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAtencion(cierre(0, recetaLarga), "doctor"));
        assertTrue(errorReceta.getMessage().contains("Indicaciones y receta"));
        assertEquals(0, inserts);
        assertNull(recibo);
        assertNull(version);
    }

    @Test void aceptaProcedimientoEnElLimiteExactoSinModificarlo() {
        String texto = "P".repeat(2000);
        assertEquals(42L, service.registrarAtencion(cierre(0, datos(texto, null, null)), "doctor"));
        verify(dao).insertarAtencion(eq(7L), isNull(), isNull(), anyString(), anyString(),
                any(), any(), any(), eq(texto), any(), any(), any(), any(), any(), eq("doctor"));
    }

    @Test void pacienteInexistenteNoGeneraAtencion() {
        pacienteExiste = false;
        assertThrows(NoSuchElementException.class, () -> service.registrarAtencion(cierre(0, datos("Control", null, null)), "doctor"));
        assertEquals(0, inserts);
    }

    @Test void rechazaCitaDeOtroPacienteOCerrada() {
        cita = Map.of("id_paciente", 8L, "estado", "CONFIRMADA");
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(cierre(0, datos("Control", 4L, null)), "doctor"));
        cita = Map.of("id_paciente", 7L, "estado", "ATENDIDA");
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(cierre(0, datos("Control", 4L, null)), "doctor"));
        assertEquals(0, inserts);
    }

    @Test void rechazaProcedimientoDeOtroPacienteOYaRealizado() {
        item = Map.of("paciente_id", 8L, "estado", "PENDIENTE");
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(cierre(0, datos("Control", null, 5L)), "doctor"));
        item = Map.of("paciente_id", 7L, "estado", "REALIZADO");
        assertThrows(IllegalArgumentException.class, () -> service.registrarAtencion(cierre(0, datos("Control", null, 5L)), "doctor"));
        assertEquals(0, inserts);
    }

    @Test void cierreActualizaSoloCitaYProcedimientoVinculados() {
        cita = Map.of("id_paciente", 7L, "estado", "CONFIRMADA");
        item = Map.of("paciente_id", 7L, "estado", "PENDIENTE");
        service.registrarAtencion(cierre(0, datos("Control", 4L, 5L)), "doctor");
        verify(dao).actualizarEstadoItemPlan(5L, "REALIZADO", 42L);
        verify(dao).actualizar("UPDATE citas SET estado = 'ATENDIDA' WHERE id_cita = ?", 4L);
    }

    @Test void evolucionaOdontogramaConCariesAObturacionAlRegistrarAtencion() {
        when(dao.obtenerOdontogramaActivo(7L, "INICIAL")).thenReturn(Map.of("id", 100L));
        when(dao.listarPiezasOdontograma(100L)).thenReturn(List.of(
                Map.of("numeroPieza", 13, "superficie", "VESTIBULAR", "condicion", "CARIES"),
                Map.of("numeroPieza", 13, "superficie", "PALATINO", "condicion", "CARIES")
        ));

        service.registrarAtencion(cierre(0, new AtencionInput(7L, null, null, null, null,
                "Curación", null, null, "Curación con resina compuesta (2+ caras)", "13",
                null, null, null, null, null)), "doctor");

        verify(dao).guardarPiezaOdontograma(eq(100L), eq(13), eq("VESTIBULAR"), eq("OBTURACION"), eq("REALIZADO"), contains("Curación"));
        verify(dao).guardarPiezaOdontograma(eq(100L), eq(13), eq("PALATINO"), eq("OBTURACION"), eq("REALIZADO"), contains("Curación"));
        verify(dao).actualizar(eq("UPDATE clinica_odontograma_versiones SET version = version + 1 WHERE odontograma_id = ?"), eq(100L));
    }
}
