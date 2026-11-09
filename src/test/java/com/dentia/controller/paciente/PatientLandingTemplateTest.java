package com.dentia.controller.paciente;

import com.dentia.controller.shared.HomeController;
import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.service.agenda.CitaAgendaService;
import com.dentia.service.agenda.DisponibilidadService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.interno.DentiaDashboardService;
import com.dentia.security.GoogleOAuthAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comprueba que la entrada pública ya no expone una portada externa. */
class PatientLandingTemplateTest {
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        prepararMvc("not-configured", "");
    }

    private void prepararMvc(String clientId, String clientSecret) {
        HomeController controller = new HomeController(mock(DentiaDashboardService.class),
                mock(ProfesionalAgendaService.class), mock(DisponibilidadService.class),
                mock(CitaAgendaService.class), mock(ICuentaPacienteDao.class),
                new GoogleOAuthAvailability(clientId, clientSecret));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void redirigeLaRaizAlLoginInterno() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/interno/login"));
    }
}
