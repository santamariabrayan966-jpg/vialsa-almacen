package com.dentia.controller.interno;

import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.clinica.ServicioDentalCatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiciosCatalogoTemplateTest {
    @Test
    void muestraCategoriaSubcategoriaPrecioYTiempoEnUnaSolaVista() throws Exception {
        var servicios = mock(ServicioDentalCatalogoService.class);
        when(servicios.listar()).thenReturn(List.of(Map.of(
                "id", 21L, "codigo", "VZ-PR-01", "nombre", "Profilaxis dental",
                "categoria", "PREVENTIVA", "subcategoria", "Higiene y prevención",
                "descripcion", "Limpieza preventiva", "precio", new BigDecimal("90.00"),
                "duracion", 30)));
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML"); resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new SpringSecurityDialect());
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(
                new CatalogoAgendaController(mock(ProfesionalAgendaService.class), servicios))
                .setViewResolvers(views).build();
        var auth = new UsernamePasswordAuthenticationToken("PRUEBA", "", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        String html;
        try {
            html = mvc.perform(get("/interno/tipos-atencion").principal(auth)
                            .requestAttr("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "prueba")))
                    .andExpect(status().isOk()).andReturn().getResponse()
                    .getContentAsString(StandardCharsets.UTF_8);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertTrue(html.contains("Servicios y precios"));
        assertTrue(html.contains("Limpieza y prevención"));
        assertTrue(html.contains("Higiene y prevención"));
        assertTrue(html.contains("Profilaxis dental"));
        assertTrue(html.contains("S/ 90.00"));
        assertTrue(html.contains("30 min aprox."));
        assertTrue(html.contains("modalServicio"));
    }
}
