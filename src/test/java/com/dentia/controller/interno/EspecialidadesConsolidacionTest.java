package com.dentia.controller.interno;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.Profesional;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.clinica.ServicioDentalCatalogoService;
import com.dentia.service.shared.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EspecialidadesConsolidacionTest {

    @Test
    void rutaEspecialidadesRedirigeAProfesionales() {
        var service = mock(ProfesionalAgendaService.class);
        var servicios = mock(ServicioDentalCatalogoService.class);
        var controller = new CatalogoAgendaController(service, servicios);

        assertEquals("redirect:/interno/profesionales", controller.especialidades());
    }

    @Test
    void guardarEspecialidadRedirigeAProfesionales() {
        var service = mock(ProfesionalAgendaService.class);
        var servicios = mock(ServicioDentalCatalogoService.class);
        var controller = new CatalogoAgendaController(service, servicios);

        String vista = controller.guardarEspecialidad(null, "Periodoncia", "Tratamiento de encías", new RedirectAttributesModelMap());
        assertEquals("redirect:/interno/profesionales", vista);
        verify(service).guardarEspecialidad(any(Especialidad.class));
    }

    @Test
    void guardarEspecialidadAjaxRetornaRespuestaExitosa() {
        var service = mock(ProfesionalAgendaService.class);
        var servicios = mock(ServicioDentalCatalogoService.class);
        when(service.guardarEspecialidad(any(Especialidad.class))).thenReturn(15L);
        var controller = new CatalogoAgendaController(service, servicios);

        ResponseEntity<?> response = controller.guardarEspecialidadAjax(null, "Implantología", "Colocación de implantes");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(15L, body.get("id"));
        assertEquals("IMPLANTOLOGÍA", body.get("nombre"));
    }

    @Test
    void profesionalListarIncluyeConteoDoctoresYEspecialidades() {
        var service = mock(ProfesionalAgendaService.class);
        var usuarioDao = mock(UsuarioDao.class);
        var fileStorage = mock(FileStorageService.class);

        Especialidad esp1 = new Especialidad(1L, "Ortodoncia", "", true);
        Especialidad esp2 = new Especialidad(2L, "Endodoncia", "", true);
        when(service.listarEspecialidades(false)).thenReturn(List.of(esp1, esp2));

        Profesional doc1 = new Profesional(10L, "Juan", "Pérez", null, null, null, null, null, true, null, null, null, List.of(esp1));
        when(service.buscarProfesionales(null, null, null)).thenReturn(List.of(doc1));

        var controller = new ProfesionalAgendaController(service, usuarioDao, fileStorage);
        var model = new ConcurrentModel();

        String vista = controller.listar(null, null, null, model);
        assertEquals("interno/profesionales/list", vista);
        assertTrue(model.containsAttribute("conteoDoctores"));
        assertTrue(model.containsAttribute("especialidades"));

        @SuppressWarnings("unchecked")
        Map<Long, Long> conteo = (Map<Long, Long>) model.getAttribute("conteoDoctores");
        assertNotNull(conteo);
        assertEquals(1L, conteo.get(1L)); // esp1 tiene 1 doctor
        assertEquals(0L, conteo.get(2L)); // esp2 tiene 0 doctores
    }

    @Test
    void agendaFragmentCargaCorrectamenteConEspecialidadesTexto() {
        var service = mock(ProfesionalAgendaService.class);
        var usuarioDao = mock(UsuarioDao.class);
        var fileStorage = mock(FileStorageService.class);

        Especialidad esp = new Especialidad(1L, "Odontología General", "", true);
        Profesional doc = new Profesional(10L, "Brayan", "Gonzales", "73781820", null, null, null, null, true, null, null, null, List.of(esp));
        when(service.buscar(10L)).thenReturn(doc);
        when(service.listarHorarios(10L)).thenReturn(List.of());
        when(service.listarBloqueos(eq(10L), any(), any())).thenReturn(List.of());
        when(service.motivosBloqueo()).thenReturn(List.of("VACACIONES"));

        var controller = new ProfesionalAgendaController(service, usuarioDao, fileStorage);
        var model = new ConcurrentModel();

        String vista = controller.agenda(10L, true, model);
        assertEquals("interno/profesionales/agenda :: agendaContent", vista);
        assertEquals(doc, model.getAttribute("profesional"));
        assertEquals("Odontología General", doc.especialidadesTexto());
    }
}
