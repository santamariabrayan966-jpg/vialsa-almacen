package com.dentia.controller.interno;

import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.model.paciente.Paciente;
import com.dentia.service.interno.PacienteInternoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacienteListTemplateTest {

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        PacienteInternoService service = mock(PacienteInternoService.class);
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(7L);
        paciente.setDni("73781820");
        paciente.setNombres("Carlos");
        paciente.setApellidos("Santamaría");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        paciente.setDireccion("Chiclayo, Lambayeque");
        paciente.setActivo(true);
        when(service.buscar(null, null)).thenReturn(List.of(paciente));

        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");

        mvc = MockMvcBuilders
                .standaloneSetup(new PacienteInternoController(service, mock(ICitaAgendaDao.class)))
                .setControllerAdvice(new HeaderTestAdvice())
                .setViewResolvers(views)
                .build();
    }

    @Test
    void renderizaRegistroYEdicionDentroDeModal() throws Exception {
        String html = mvc.perform(get("/interno/pacientes"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(html.contains("id=\"modalPaciente\""));
        assertTrue(html.contains("id=\"btnConsultarDni\""));
        assertTrue(html.contains("Ubicación / dirección"));
        assertTrue(html.contains("Celular <span class=\"text-secondary fw-normal\">(opcional)</span>"));
    }

    @Test
    void renderizaBotonAtenderDirectoHaciaClinica() throws Exception {
        String html = mvc.perform(get("/interno/pacientes"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(html.contains("href=\"/interno/clinica?pacienteId=7\""));
        assertTrue(html.contains("Atender"));
    }

    @ControllerAdvice
    static class HeaderTestAdvice {
        @ModelAttribute
        void cabecera(Model model) {
            model.addAttribute("usuarioActualNombre", "Pruebas");
            model.addAttribute("usuarioActualRol", "ADMIN");
            model.addAttribute("permisosUsuario", java.util.Map.of());
            model.addAttribute("logoPrincipalUrl", "");
            model.addAttribute("marcaConfig", null);
            model.addAttribute("_csrf", new CsrfPrueba());
        }
    }

    static class CsrfPrueba {
        public String getParameterName() { return "_csrf"; }
        public String getToken() { return "test-token"; }
    }
}
