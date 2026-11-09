package com.dentia.service.clinica;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.config.ClinicaSchemaConfig;
import com.dentia.dao.Jdbc.JdbcClinicaDao;
import com.dentia.dto.clinica.ClinicaRequests.*;
import com.dentia.service.interno.ClinicaInternaService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Solo la instancia efímera creada por scripts/test-clinica-mysql.ps1. No carga Spring Boot ni application.properties. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClinicaMysqlIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private JdbcClinicaDao dao;
    private AtencionClinicaService atenciones;
    private OdontogramaService odontogramas;
    private ClinicaInternaService clinica;
    private long paciente;

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
    }

    @BeforeAll void crearEsquemaAislado() throws Exception {
        String url = System.getenv("DENTIA_TEST_MYSQL_URL");
        String expectedData = System.getenv("DENTIA_TEST_MYSQL_DATADIR");
        assertNotNull(url, "Ejecuta scripts/test-clinica-mysql.ps1; no se usa la configuración real.");
        assertNotNull(expectedData);
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/dentia_test_[a-f0-9]{32}\\?.*"));
        Path expected = Path.of(expectedData).toAbsolutePath().normalize();
        Path allowed = Path.of("target", "dentia-mysql-checks").toAbsolutePath().normalize();
        assertTrue(expected.startsWith(allowed), "El directorio debe estar en target/dentia-mysql-checks.");
        var ds = new DriverManagerDataSource(url, "root", "");
        jdbc = new JdbcTemplate(ds);
        Path actual = Path.of(jdbc.queryForObject("SELECT @@datadir", String.class)).toAbsolutePath().normalize();
        assertEquals(expected, actual, "No se escribirá en otra instancia MySQL.");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", Integer.class),
                "Se exige un esquema vacío y exclusivo.");
        // Padres sintéticos; Gestión y Clínica usan sus migraciones reales.
        jdbc.execute("CREATE TABLE pacientes (id_paciente BIGINT AUTO_INCREMENT PRIMARY KEY, nombres VARCHAR(100) NOT NULL DEFAULT 'Paciente', apellidos VARCHAR(100) NOT NULL DEFAULT 'Sintético', dni VARCHAR(20), fecha_nacimiento DATE, celular VARCHAR(20)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE profesionales (id_profesional BIGINT AUTO_INCREMENT PRIMARY KEY, id_usuario INT NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE citas (id_cita BIGINT AUTO_INCREMENT PRIMARY KEY, id_paciente BIGINT NOT NULL, estado VARCHAR(30) NOT NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE usuarios (idUsuario INT PRIMARY KEY, NombreUsuario VARCHAR(100), Telefono VARCHAR(20)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE permisos (idPermiso INT AUTO_INCREMENT PRIMARY KEY,idRol INT,modulo VARCHAR(100),puedeAcceder BOOLEAN) ENGINE=InnoDB");
        var gestion = new ResourceDatabasePopulator(new ClassPathResource("db/migration/dentia/V2026_09_03_01__gestion_interna.sql"));
        gestion.setSqlScriptEncoding("UTF-8");
        gestion.execute(ds);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> ds);
        context.registerBean(JdbcTemplate.class, () -> jdbc);
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules());
        context.register(Transactions.class, JdbcClinicaDao.class, AtencionClinicaService.class,
                OdontogramaService.class, ClinicaInternaService.class, ClinicaSchemaConfig.class);
        context.refresh();
        context.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments());
        dao = context.getBean(JdbcClinicaDao.class);
        atenciones = context.getBean(AtencionClinicaService.class);
        odontogramas = context.getBean(OdontogramaService.class);
        clinica = context.getBean(ClinicaInternaService.class);
    }

    @AfterAll void cerrarContexto() { if (context != null) context.close(); }
    @BeforeEach void nuevoPaciente() { paciente = dao.insertar("INSERT INTO pacientes VALUES ()"); }
    private AtencionInput datos(String detalle, Long cita, Long item) {
        return new AtencionInput(paciente, null, cita, null, "Control sintético", "Nota de prueba",
                null, null, detalle, "16", null, null, null, null, item);
    }
    private FinalizarAtencionInput cierre(long version, AtencionInput input) {
        return new FinalizarAtencionInput(version, UUID.randomUUID().toString(), input);
    }
    private void guardar(long version, String detalle) {
        atenciones.guardarBorrador(new BorradorAtencionInput(paciente, version, datos(detalle, null, null)), "test");
    }
    private long revision() { return ((Number) atenciones.obtenerBorrador(paciente).get("version")).longValue(); }
    private int numeroAtenciones() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinica_atenciones WHERE paciente_id=?", Integer.class, paciente);
    }
    private long crearOdontograma() {
        return dao.insertar("INSERT INTO clinica_odontogramas (paciente_id,tipo,fecha,creado_por) VALUES (?,'INICIAL',CURRENT_DATE,'test')", paciente);
    }
    private long crearPlanPresupuestable(String total) {
        long plan = dao.insertar("INSERT INTO clinica_planes_tratamiento (paciente_id,titulo,costo_total,creado_por) VALUES (?,'Plan sintético',?,'test')",
                paciente, new BigDecimal(total));
        dao.insertarItemPlan(plan, null, null, 16, "Procedimiento sintético", new BigDecimal("100.00"), 1, "PENDIENTE");
        dao.insertarItemPlan(plan, null, null, null, "Control sintético", new BigDecimal("50.00"), 2, "PENDIENTE");
        return plan;
    }
    private int numeroPresupuestos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM gestion_presupuestos WHERE paciente_id=?", Integer.class, paciente);
    }
    private String intentar(Runnable action) {
        try { action.run(); return "OK"; }
        catch (OdontogramaService.EdicionConcurrenteException e) { return "CONFLICTO"; }
    }
    private <T> List<T> simultaneos(Supplier<T> first, Supplier<T> second) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (var task : List.of(first, second)) futures.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No arrancó la prueba concurrente.");
                return task.get();
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test void migracionesReejecutablesSinPerderBorrador() throws Exception {
        guardar(0, "Borrador conservado");
        context.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments());
        assertEquals(1L, revision());
        assertTrue(atenciones.obtenerBorrador(paciente).get("contenido").toString().contains("Borrador conservado"));
    }

    @Test void reinicializacionConservaCatalogosPersonalizadosEInactivos() throws Exception {
        jdbc.update("UPDATE clinica_catalogo_diagnosticos SET nombre='Nombre local', descripcion='Descripción local', categoria='LOCAL', activo=0 WHERE codigo_cie10='K02.0'");
        jdbc.update("UPDATE clinica_catalogo_tratamientos SET nombre='Procedimiento local', descripcion='Detalle local', categoria='LOCAL', precio_sugerido=137.50, duracion_estimada_min=55, activo=0 WHERE codigo='VZ-PM-01'");
        jdbc.update("INSERT INTO clinica_catalogo_diagnosticos (codigo_cie10,nombre) VALUES ('TEST-LOCAL','Diagnóstico sintético propio')");
        jdbc.update("INSERT INTO clinica_catalogo_tratamientos (codigo,nombre,categoria,precio_sugerido) VALUES ('TEST-LOCAL','Tratamiento sintético propio','LOCAL',245.75)");
        var diagnosticosAntes = jdbc.queryForList("SELECT * FROM clinica_catalogo_diagnosticos ORDER BY id");
        var tratamientosAntes = jdbc.queryForList("SELECT * FROM clinica_catalogo_tratamientos ORDER BY id");

        for (int reinicio = 0; reinicio < 2; reinicio++) {
            context.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments());
            assertEquals(diagnosticosAntes, jdbc.queryForList("SELECT * FROM clinica_catalogo_diagnosticos ORDER BY id"));
            assertEquals(tratamientosAntes, jdbc.queryForList("SELECT * FROM clinica_catalogo_tratamientos ORDER BY id"));
        }
    }

    @Test void reinicializacionAgregaCodigosBaseAusentesSinDuplicar() throws Exception {
        String diagnosticoSql = "SELECT codigo_cie10,nombre,descripcion,categoria,activo FROM clinica_catalogo_diagnosticos WHERE codigo_cie10='K02.1'";
        String tratamientoSql = "SELECT codigo,nombre,categoria,descripcion,precio_sugerido,duracion_estimada_min,activo FROM clinica_catalogo_tratamientos WHERE codigo='VZ-PM-02'";
        var diagnosticoBase = jdbc.queryForMap(diagnosticoSql);
        var tratamientoBase = jdbc.queryForMap(tratamientoSql);
        int diagnosticosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM clinica_catalogo_diagnosticos", Integer.class);
        int tratamientosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM clinica_catalogo_tratamientos", Integer.class);
        // Solo filas semilla sin referencias en la base efímera verificada al inicio.
        assertEquals(1, jdbc.update("DELETE FROM clinica_catalogo_diagnosticos WHERE codigo_cie10='K02.1'"));
        assertEquals(1, jdbc.update("DELETE FROM clinica_catalogo_tratamientos WHERE codigo='VZ-PM-02'"));

        for (int reinicio = 0; reinicio < 2; reinicio++) {
            context.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments());
            assertEquals(diagnosticoBase, jdbc.queryForMap(diagnosticoSql));
            assertEquals(tratamientoBase, jdbc.queryForMap(tratamientoSql));
            assertEquals(diagnosticosAntes, jdbc.queryForObject("SELECT COUNT(*) FROM clinica_catalogo_diagnosticos", Integer.class));
            assertEquals(tratamientosAntes, jdbc.queryForObject("SELECT COUNT(*) FROM clinica_catalogo_tratamientos", Integer.class));
        }
    }

    @Test void persisteReabreCierraYReintentaSinAfectarBorradorPosterior() {
        guardar(0, null);
        var reopened = new AtencionClinicaService(new JdbcClinicaDao(new JdbcTemplate(context.getBean(DataSource.class))),
                new ObjectMapper().findAndRegisterModules());
        assertEquals(1L, ((Number) reopened.obtenerBorrador(paciente).get("version")).longValue());
        var request = cierre(1, datos("Restauración sintética", null, null));
        long id = atenciones.registrarAtencion(request, "test");
        assertEquals(2L, revision());
        assertTrue(((JsonNode) atenciones.obtenerBorrador(paciente).get("contenido")).isNull());
        guardar(2, "Nueva sesión");
        assertEquals(id, atenciones.registrarAtencion(request, "test"));
        assertEquals(1, numeroAtenciones());
        assertEquals(3L, revision());
        assertTrue(atenciones.obtenerBorrador(paciente).get("contenido").toString().contains("Nueva sesión"));
    }

    @Test void dosGuardadosConcurrentesSoloAceptanUnaVersion() throws Exception {
        var results = simultaneos(() -> intentar(() -> guardar(0, "A")), () -> intentar(() -> guardar(0, "B")));
        assertEquals(Set.of("OK", "CONFLICTO"), new HashSet<>(results));
        assertEquals(1L, revision());
    }

    @Test void dobleFinalizacionConcurrenteCreaUnaSolaAtencion() throws Exception {
        var request = cierre(0, datos("Control", null, null));
        var ids = simultaneos(() -> atenciones.registrarAtencion(request, "test"), () -> atenciones.registrarAtencion(request, "test"));
        assertEquals(ids.get(0), ids.get(1));
        assertEquals(1, numeroAtenciones());
        assertEquals(1L, revision());
    }

    @Test void guardarContraFinalizarNoPierdeCambios() throws Exception {
        var request = cierre(0, datos("Control", null, null));
        var results = simultaneos(() -> intentar(() -> guardar(0, "Borrador concurrente")),
                () -> intentar(() -> atenciones.registrarAtencion(request, "test")));
        assertEquals(Set.of("OK", "CONFLICTO"), new HashSet<>(results));
        assertEquals(1L, revision());
        assertEquals(results.get(1).equals("OK") ? 1 : 0, numeroAtenciones());
        if (results.get(0).equals("OK"))
            assertTrue(atenciones.obtenerBorrador(paciente).get("contenido").toString().contains("Borrador concurrente"));
    }

    @Test void falloAlGuardarReciboRevierteAtencionCitaPlanYBorrador() {
        long cita = dao.insertar("INSERT INTO citas (id_paciente,estado) VALUES (?,'CONFIRMADA')", paciente);
        long plan = dao.insertar("INSERT INTO clinica_planes_tratamiento (paciente_id,titulo,creado_por) VALUES (?,'Plan sintético','test')", paciente);
        long item = dao.insertar("INSERT INTO clinica_plan_tratamiento_items (plan_id,descripcion) VALUES (?,'Procedimiento sintético')", plan);
        guardar(0, "Borrador previo");
        jdbc.execute("CREATE TRIGGER dentia_test_rechazar_recibo BEFORE INSERT ON clinica_atencion_solicitudes FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo intencional de prueba'");
        try {
            assertThrows(DataAccessException.class, () -> atenciones.registrarAtencion(cierre(1, datos("Control", cita, item)), "test"));
        } finally { jdbc.execute("DROP TRIGGER dentia_test_rechazar_recibo"); }
        assertEquals(0, numeroAtenciones());
        assertEquals(1L, revision());
        assertTrue(atenciones.obtenerBorrador(paciente).get("contenido").toString().contains("Borrador previo"));
        assertEquals("CONFIRMADA", jdbc.queryForObject("SELECT estado FROM citas WHERE id_cita=?", String.class, cita));
        assertEquals("PENDIENTE", jdbc.queryForObject("SELECT estado FROM clinica_plan_tratamiento_items WHERE id=?", String.class, item));
        assertNull(jdbc.queryForObject("SELECT atencion_id FROM clinica_plan_tratamiento_items WHERE id=?", Long.class, item));
    }

    @Test void odontogramaPersisteGeometriaHallazgosHistorialYVersion() {
        long id = crearOdontograma();
        odontogramas.registrar(new OdontogramaPiezaInput(id, 16, "OCLUSAL", "CARIES", "PATOLOGICO", "Prueba sintética", 0L), "test");
        odontogramas.configurar(new ConfiguracionOdontogramaInput(id, 1L, List.of(new PosicionPiezaInput(16, 12.5, "PRESENTE"))), "test");
        assertEquals(12.5, jdbc.queryForObject("SELECT inclinacion FROM clinica_odontograma_posiciones WHERE odontograma_id=?", Double.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM clinica_odontograma_piezas WHERE odontograma_id=? AND condicion='CARIES'", Integer.class, id));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM clinica_odontograma_historial WHERE odontograma_id=?", Integer.class, id));
        assertEquals(2L, jdbc.queryForObject("SELECT version FROM clinica_odontograma_versiones WHERE odontograma_id=?", Long.class, id));
        assertThrows(OdontogramaService.EdicionConcurrenteException.class,
                () -> odontogramas.limpiar(new LimpiarPiezaInput(id, 16, 1L), "otro"));
    }

    @Test void odontogramaConcurrenteRechazaSobrescritura() throws Exception {
        long id = crearOdontograma();
        var results = simultaneos(
                () -> intentar(() -> odontogramas.registrar(new OdontogramaPiezaInput(id, 16, "OCLUSAL", "CARIES", "PATOLOGICO", "A", 0L), "a")),
                () -> intentar(() -> odontogramas.registrar(new OdontogramaPiezaInput(id, 16, "OCLUSAL", "SANO", "SANO", "B", 0L), "b")));
        assertEquals(Set.of("OK", "CONFLICTO"), new HashSet<>(results));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM clinica_odontograma_historial WHERE odontograma_id=?", Integer.class, id));
    }

    @Test void doblePresupuestoConcurrenteYReintentoConservanUnSoloRegistro() throws Exception {
        long plan = crearPlanPresupuestable("150.00");
        var ids = simultaneos(() -> clinica.generarPresupuestoDesdePlan(plan, "test"),
                () -> clinica.generarPresupuestoDesdePlan(plan, "test"));
        assertEquals(ids.get(0), ids.get(1));
        assertEquals(ids.get(0).longValue(), clinica.generarPresupuestoDesdePlan(plan, "reintento"));
        assertEquals(1, numeroPresupuestos());
        long id = ids.get(0);
        assertEquals(id, jdbc.queryForObject("SELECT presupuesto_id FROM clinica_planes_tratamiento WHERE id=?", Long.class, plan));
        assertEquals("APROBADO", jdbc.queryForObject("SELECT estado FROM clinica_planes_tratamiento WHERE id=?", String.class, plan));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM gestion_presupuesto_items WHERE presupuesto_id=?", Integer.class, id));
        assertEquals(new BigDecimal("150.00"), jdbc.queryForObject("SELECT total FROM gestion_presupuestos WHERE id=?", BigDecimal.class, id));
        assertEquals(List.of("[Pza 16] Procedimiento sintético", "Control sintético"),
                jdbc.queryForList("SELECT descripcion FROM gestion_presupuesto_items WHERE presupuesto_id=? ORDER BY id", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM gestion_historial WHERE modulo='presupuestos' AND registro_id=?", Integer.class, id));
    }

    @Test void falloAlVincularPresupuestoRevierteCabeceraItemsAuditoriaYPlan() {
        long plan = crearPlanPresupuestable("150.00");
        int itemsAntes = jdbc.queryForObject("SELECT COUNT(*) FROM gestion_presupuesto_items", Integer.class);
        int historialAntes = jdbc.queryForObject("SELECT COUNT(*) FROM gestion_historial", Integer.class);
        jdbc.execute("CREATE TRIGGER dentia_test_rechazar_vinculo BEFORE UPDATE ON clinica_planes_tratamiento FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo intencional de prueba'");
        try {
            assertThrows(DataAccessException.class, () -> clinica.generarPresupuestoDesdePlan(plan, "test"));
        } finally { jdbc.execute("DROP TRIGGER dentia_test_rechazar_vinculo"); }
        assertEquals(0, numeroPresupuestos());
        assertEquals(itemsAntes, jdbc.queryForObject("SELECT COUNT(*) FROM gestion_presupuesto_items", Integer.class));
        assertEquals(historialAntes, jdbc.queryForObject("SELECT COUNT(*) FROM gestion_historial", Integer.class));
        assertNull(jdbc.queryForObject("SELECT presupuesto_id FROM clinica_planes_tratamiento WHERE id=?", Long.class, plan));
        assertEquals("BORRADOR", jdbc.queryForObject("SELECT estado FROM clinica_planes_tratamiento WHERE id=?", String.class, plan));
        long id = clinica.generarPresupuestoDesdePlan(plan, "test");
        assertEquals(id, clinica.generarPresupuestoDesdePlan(plan, "test"));
        assertEquals(1, numeroPresupuestos());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-50.00"})
    void totalNoPositivoSeRecalculaSinAlterarLaSumaDeItems(String total) {
        long id = clinica.generarPresupuestoDesdePlan(crearPlanPresupuestable(total), null);
        BigDecimal suma = jdbc.queryForObject("SELECT SUM(cantidad * precio) FROM gestion_presupuesto_items WHERE presupuesto_id=?", BigDecimal.class, id);
        assertEquals(new BigDecimal("150.00"), suma);
        assertEquals(suma, jdbc.queryForObject("SELECT total FROM gestion_presupuestos WHERE id=?", BigDecimal.class, id));
        assertEquals("SISTEMA", jdbc.queryForObject("SELECT creado_por FROM gestion_presupuestos WHERE id=?", String.class, id));
        assertEquals("SISTEMA", jdbc.queryForObject("SELECT usuario FROM gestion_historial WHERE modulo='presupuestos' AND registro_id=?", String.class, id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void precioInvalidoNoCreaPresupuestoNiModificaPlan(String precio) {
        long plan = crearPlanPresupuestable("150.00");
        jdbc.update("UPDATE clinica_plan_tratamiento_items SET precio=? WHERE plan_id=? AND orden_fase=2", new BigDecimal(precio), plan);
        assertThrows(IllegalArgumentException.class, () -> clinica.generarPresupuestoDesdePlan(plan, "test"));
        assertEquals(0, numeroPresupuestos());
        assertNull(jdbc.queryForObject("SELECT presupuesto_id FROM clinica_planes_tratamiento WHERE id=?", Long.class, plan));
        assertEquals("BORRADOR", jdbc.queryForObject("SELECT estado FROM clinica_planes_tratamiento WHERE id=?", String.class, plan));
    }
    @Test void atencionDirectaGeneraCargoUnaVezSinInventarPago() {
        var input = new AtencionInput(paciente, null, null, null, "Control sintético", null, null, null,
                "Resina", "16", null, null, null, null, null, new BigDecimal("85.50"));
        var request = cierre(0, input);
        long id = atenciones.registrarAtencion(request, "test");
        assertEquals(id, atenciones.registrarAtencion(request, "test"));
        assertEquals(1, numeroAtenciones());
        assertEquals(1, numeroPresupuestos());
        long presupuesto = jdbc.queryForObject("SELECT id FROM gestion_presupuestos WHERE paciente_id=?",Long.class,paciente);
        assertEquals(new BigDecimal("85.50"),jdbc.queryForObject("SELECT total FROM gestion_presupuestos WHERE id=?",BigDecimal.class,presupuesto));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM gestion_pagos WHERE presupuesto_id=?",Integer.class,presupuesto));
    }

    @Test void falloDeCargoRevierteAtencionCompleta() {
        var input = new AtencionInput(paciente, null, null, null, "Control sintético", null, null, null,
                "Resina", "16", null, null, null, null, null, new BigDecimal("85.50"));
        jdbc.execute("CREATE TRIGGER dentia_test_rechazar_cargo BEFORE INSERT ON gestion_presupuestos FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo intencional'");
        try { assertThrows(DataAccessException.class, () -> atenciones.registrarAtencion(cierre(0,input),"test")); }
        finally { jdbc.execute("DROP TRIGGER dentia_test_rechazar_cargo"); }
        assertEquals(0,numeroAtenciones()); assertEquals(0,numeroPresupuestos()); assertEquals(0,revision());
    }
    @Test void filtrosPersonalesUsanElDoctorAunqueOtroUsuarioRegistreLaAtencion() {
        jdbc.update("INSERT INTO usuarios (idUsuario,NombreUsuario) VALUES (9901,'doctor_prueba'),(9902,'otro_doctor')");
        long profesional = dao.insertar("INSERT INTO profesionales (id_usuario) VALUES (9901)");
        var input = new AtencionInput(paciente, profesional, null, null, "Control", null, null, null,
                "Control sintético", "16", null, null, null, null, null);
        atenciones.registrarAtencion(cierre(0,input), "recepcion");
        assertTrue(clinica.misPacientes("doctor_prueba").contains(paciente));
        assertFalse(clinica.misPacientes("otro_doctor").contains(paciente));
        assertEquals(1,clinica.misAtenciones("doctor_prueba").size());
        assertTrue(clinica.misAtenciones("recepcion").isEmpty());
        assertEquals(1,numeroAtenciones());
    }
}
