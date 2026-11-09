package com.dentia.controller.interno;

import com.dentia.dto.clinica.ClinicaRequests.FinalizarAtencionInput;
import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.clinica.OdontogramaService.EdicionConcurrenteException;
import com.dentia.service.interno.ClinicaInternaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClinicaAtencionApiTest {
    private ClinicaInternaService service;
    private CustomAuthorizationService permisos;
    private MockMvc mvc;
    private final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("doctor", "", List.of());
    private final String payload = """
            {"version":3,"solicitudId":"a5a171ca-94b1-413a-bf1d-a21c432f9a92",
             "contenido":{"pacienteId":7,"planProcedimiento":"Control"}}
            """;

    @BeforeEach void preparar() {
        service = mock(ClinicaInternaService.class);
        permisos = mock(CustomAuthorizationService.class);
        when(permisos.tieneAcceso(auth, "atenciones")).thenReturn(true);
        mvc = MockMvcBuilders.standaloneSetup(new ClinicaInternaController(service, permisos))
                .setControllerAdvice(new ClinicaExceptionHandler()).build();
    }

    @Test void contratoDeCierreConservaVersionClavePacienteYActor() throws Exception {
        when(service.registrarAtencion(any(), eq("doctor"))).thenReturn(42L);
        mvc.perform(post("/interno/clinica/api/atenciones").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42));
        verify(service).registrarAtencion(argThat((FinalizarAtencionInput req) ->
                req.version() == 3L && req.contenido().pacienteId() == 7L
                        && req.solicitudId().equals("a5a171ca-94b1-413a-bf1d-a21c432f9a92")), eq("doctor"));
    }

    @Test void conflictoSeExponeComo409SinFalsoExito() throws Exception {
        when(service.registrarAtencion(any(), any())).thenThrow(new EdicionConcurrenteException());
        mvc.perform(post("/interno/clinica/api/atenciones").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").isString());
    }

    @Test void textoClinicoExcesivoDevuelve400ConCampoYLimite() throws Exception {
        when(service.registrarAtencion(any(), any()))
                .thenThrow(new IllegalArgumentException("Procedimiento realizado supera el límite de 2000 caracteres."));
        mvc.perform(post("/interno/clinica/api/atenciones").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Procedimiento realizado supera el límite de 2000 caracteres."));
    }

    @Test void permisoDeOtroModuloNoAutorizaGuardarNiFinalizar() throws Exception {
        when(permisos.tieneAcceso(auth, "atenciones")).thenReturn(false);
        when(permisos.tieneAcceso(auth, "odontograma")).thenReturn(true);
        mvc.perform(post("/interno/clinica/api/atenciones").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(post("/interno/clinica/api/atenciones/borrador").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"pacienteId":7,"version":0,"contenido":{"pacienteId":7}}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void jsonInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/interno/clinica/api/atenciones").principal(auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
