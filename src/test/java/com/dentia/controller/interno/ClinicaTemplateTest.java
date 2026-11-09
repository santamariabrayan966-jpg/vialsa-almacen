package com.dentia.controller.interno;

import com.dentia.security.CustomAuthorizationService;
import com.dentia.security.GlobalPermissionsInjector;
import com.dentia.service.interno.ClinicaInternaService;
import com.dentia.service.interno.PanelColoresConfigService;
import com.dentia.service.interno.PermisoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renderiza las plantillas y los consejos MVC reales, sin arrancar la aplicación ni conectar a una base. */
class ClinicaTemplateTest {
    private MockMvc mvc;
    private ClinicaInternaService service;
    private final Set<String> permisos = new HashSet<>();
    private final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("PRUEBA SIN DATOS REALES", "", List.of());

    private static String permiso(String modulo) {
        return switch (modulo) {
            case "cuentas-pagos" -> "pagos";
            case "historial-citas" -> "citas";
            default -> modulo.replace('-', '_');
        };
    }

    @BeforeEach void preparar() {
        service = mock(ClinicaInternaService.class);
        when(service.listarProfesionales()).thenReturn(List.of(Map.of("id", 9L, "nombre", "Profesional <prueba> & equipo")));
        var authorization = mock(CustomAuthorizationService.class);
        when(authorization.tieneAcceso(eq(auth), anyString())).thenAnswer(i -> permisos.contains(i.getArgument(1)));
        var permissionService = mock(PermisoService.class);
        when(permissionService.getPermisos(auth)).thenAnswer(i -> {
            Map<String, Boolean> values = new HashMap<>();
            permisos.forEach(p -> values.put(p, true));
            return values;
        });
        var globalPermissions = new GlobalPermissionsInjector();
        ReflectionTestUtils.setField(globalPermissions, "permisoService", permissionService);
        var header = new HeaderModelAdvice(mock(PanelColoresConfigService.class));
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver);
        engine.addDialect(new SpringSecurityDialect());
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        var jdbcReportes = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        when(jdbcReportes.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);
        when(jdbcReportes.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        mvc = MockMvcBuilders.standaloneSetup(new ClinicaInternaController(service, authorization),
                new ReportesClinicosController(authorization, mock(com.dentia.service.agenda.ProfesionalAgendaService.class), jdbcReportes))
                .setControllerAdvice(globalPermissions, header, new ClinicaExceptionHandler())
                .setViewResolvers(views).build();
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private ResultActions pagina(String path, String... params) throws Exception {
        var request = get(path).principal(auth)
                .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-sintetico"));
        for (int i = 0; i < params.length; i += 2) request.param(params[i], params[i + 1]);
        return mvc.perform(request);
    }
    private String html(String modulo, String... params) throws Exception {
        return pagina("/interno/clinica/" + modulo, params)
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
    private String tag(String html, String selector) {
        var matcher = Pattern.compile("<[^>]+\\b" + selector + "[^>]*>").matcher(html);
        assertTrue(matcher.find(), "No se encontró " + selector);
        return matcher.group();
    }

    @ParameterizedTest
    @ValueSource(strings = {"odontograma", "historia-clinica", "diagnosticos",
            "plan-tratamiento", "atenciones", "cuentas-pagos", "historial-citas"})
    void renderizaCadaModuloConSuPermisoYUnaSolaPestana(String modulo) throws Exception {
        permisos.add(permiso(modulo));
        String rendered = html(modulo);
        assertTrue(rendered.contains("csrf-sintetico"));
        assertTrue(rendered.contains("Profesional &lt;prueba&gt; &amp; equipo"));
        assertTrue(rendered.contains("href=\"/interno/clinica\""));
        assertFalse(rendered.contains("Cita Activa #"));
        assertFalse(rendered.contains("th:data-modulo"));
        assertFalse(rendered.contains("Cargando evoluciones"));
        assertTrue(rendered.contains("Selecciona un paciente para consultar sus atenciones y evoluciones."));
        for (String other : ClinicaInternaService.MODULOS_CLINICOS) {
            var tab = Pattern.compile("<button[^>]*data-modulo=\"" + other + "\"[^>]*>").matcher(rendered);
            assertEquals(other.equals(modulo), tab.find(), "Pestaña permitida: " + other);
            if (other.equals(modulo)) assertTrue(tab.group().contains("active"));
            String panel = tag(rendered, "id=\"panel-" + other + "\"");
            assertEquals(!other.equals(modulo), panel.contains("hidden"), "Panel inicial: " + other);
        }
    }

    @Test void pacienteYCitaNumericosSeRenderizanSinErrorDeTipo() throws Exception {
        permisos.add("atenciones");
        String rendered = html("atenciones", "pacienteId", "7", "citaId", "42");
        assertTrue(rendered.contains("data-paciente-id=\"7\""));
        assertTrue(rendered.contains("data-cita-id=\"42\""));
        assertTrue(rendered.contains("Cita Activa #"));
        assertTrue(tag(rendered, "id=\"atencion-cita-id\"").contains("value=\"42\""));
        assertTrue(tag(rendered, "id=\"btn-recargar-atencion-conflicto\"").contains("hidden"));
        String piezasTratadas = tag(rendered, "name=\"piezasTratadas\"");
        assertTrue(piezasTratadas.contains("maxlength=\"100\""));
        assertFalse(piezasTratadas.contains(" value="));
        assertTrue(tag(rendered, "name=\"subjetivo\"").contains("maxlength=\"1500\""));
        assertTrue(tag(rendered, "name=\"objetivo\"").contains("maxlength=\"1500\""));
        assertTrue(tag(rendered, "name=\"evaluacion\"").contains("maxlength=\"1500\""));
        assertTrue(rendered.contains("E (Evaluación) - Valoración clínica"));
        String procedimiento = tag(rendered, "name=\"planProcedimiento\"");
        assertTrue(procedimiento.contains("maxlength=\"2000\""));
        assertTrue(procedimiento.contains("required"));
        String materiales = tag(rendered, "name=\"materiales\"");
        assertTrue(materiales.contains("maxlength=\"500\""));
        assertFalse(materiales.contains(" value="));
        assertTrue(tag(rendered, "name=\"indicacionesReceta\"").contains("maxlength=\"2000\""));
    }

    @Test void atencionDirectaSinCitaHabilitaBotonYSelectorDeProfesional() throws Exception {
        permisos.add("atenciones");
        String rendered = html("atenciones", "pacienteId", "7");
        assertTrue(rendered.contains("data-paciente-id=\"7\""));
        assertFalse(rendered.contains("Cita Activa #"));
        assertTrue(rendered.contains("id=\"btn-hero-atencion-directa\""));
        assertTrue(rendered.contains("id=\"btn-nueva-atencion\""));
        assertTrue(rendered.contains("id=\"atencion-modal-directo-notice\""));
        assertTrue(rendered.contains("id=\"atencion-profesional-id\""));
    }

    @Test void entradaGeneralConservaPacienteYCitaYSeleccionaModuloPermitido() throws Exception {
        permisos.add("atenciones");
        pagina("/interno/clinica", "pacienteId", "7", "citaId", "42")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/interno/clinica/atenciones?pacienteId=7&citaId=42"));
        pagina("/interno/clinica/").andExpect(redirectedUrl("/interno/clinica/atenciones"));
        verifyNoInteractions(service);
    }

    @Test void entradaGeneralPriorizaOdontogramaCuandoEstaPermitido() throws Exception {
        permisos.addAll(Set.of("atenciones", "odontograma"));
        pagina("/interno/clinica").andExpect(redirectedUrl("/interno/clinica/odontograma"));
    }

    @Test void moduloTratamientosRedirigeAPlanTratamiento() throws Exception {
        permisos.add("tratamientos");
        pagina("/interno/clinica/tratamientos", "pacienteId", "15")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/interno/clinica/plan-tratamiento?pacienteId=15"));
    }

    @Test void rechazaEntradasSinPermisoYModuloDesconocido() throws Exception {
        pagina("/interno/clinica").andExpect(status().isForbidden());
        pagina("/interno/clinica/atenciones").andExpect(status().isForbidden());
        pagina("/interno/clinica/desconocido").andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test void generaVistaDeInspeccionSinPacientesYConTodosLosModulos() throws Exception {
        ClinicaInternaService.MODULOS_CLINICOS.forEach(m -> permisos.add(permiso(m)));
        permisos.addAll(Set.of("odontograma_2d", "odontograma_3d"));
        String rendered = html("odontograma");
        assertTrue(rendered.contains("Selecciona un paciente"));
        assertTrue(rendered.contains("/js/dentia/atencion-session.js?v=4.2"));
        assertTrue(rendered.contains("/js/dentia/clinica.js?v=4.7"));
        assertTrue(rendered.contains("Dientes permanentes"));
        assertTrue(rendered.contains("Dientes de leche"));
        for (int cuadrante = 1; cuadrante <= 8; cuadrante++) {
            assertTrue(rendered.contains("id=\"odonto-q" + cuadrante + "\""));
        }
        assertTrue(rendered.contains("id=\"btn-guardar-odontograma-2d\""));
        assertTrue(rendered.contains("id=\"modal-detalle-pieza\""));
        assertTrue(rendered.contains("id=\"studio-3d-workspace\""));
        assertTrue(rendered.contains("/vendor/three/GLTFLoader.js"));
        Path destination = Path.of("target", "dentia-verification", "preview", "clinica.html");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, rendered, StandardCharsets.UTF_8);
    }

    @Test void planTratamientoConstructorRenderizaComponentesCompletos() throws Exception {
        permisos.add("plan_tratamiento");
        String rendered = html("plan-tratamiento");
        assertTrue(rendered.contains("id=\"plan-builder-container\""));
        assertTrue(rendered.contains("id=\"mob-tab-btn-cat\""));
        assertTrue(rendered.contains("id=\"mob-tab-btn-cart\""));
        assertTrue(rendered.contains("id=\"mobile-sticky-cart-bar\""));
        assertTrue(rendered.contains("id=\"builder-toggle-descuentos\""));
        assertTrue(rendered.contains("id=\"discount-mode-selector\""));
        assertTrue(rendered.contains("id=\"input-global-discount-val\""));
        assertTrue(rendered.contains("id=\"builder-summary-subtotal\""));
        assertTrue(rendered.contains("id=\"builder-summary-discount\""));
        assertTrue(rendered.contains("id=\"btn-builder-aprobar-deuda\""));
    }
    @Test void reportesRenderizanGraficosConBaseVacia() throws Exception {
        permisos.add("reportes");
        pagina("/interno/reportes").andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reportes clínicos")));
    }
    @Test void rolSolo2dNoRecibeVisor3d() throws Exception {
        permisos.addAll(Set.of("odontograma", "odontograma_2d"));
        String rendered = html("odontograma");
        assertTrue(rendered.contains("id=\"studio-2d-container\""));
        assertFalse(rendered.contains("id=\"studio-3d-workspace\""));
    }
    @Test void rolSolo3dNoRecibeVista2d() throws Exception {
        permisos.addAll(Set.of("odontograma", "odontograma_3d"));
        String rendered = html("odontograma");
        assertTrue(rendered.contains("id=\"studio-3d-workspace\""));
        assertFalse(rendered.contains("id=\"studio-2d-container\""));
    }
}
