package com.dentia.gestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dentia.dao.Jdbc.JdbcGestionDao;
import com.dentia.dto.gestion.GestionRequests.*;
import com.dentia.service.interno.GestionInternaService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.junit.jupiter.api.Tag("integration")
@SpringBootTest(properties={"dentia.bootstrap-admin.enabled=false","dentia.gestion.initialize-schema=false"})
@AutoConfigureMockMvc
@Transactional
class GestionHttpTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcGestionDao dao;
    @Autowired GestionInternaService service;
    MockHttpSession session;String csrf;
    @BeforeEach void sesion() throws Exception {
        session=sesion("ADMIN");
        String html=mvc.perform(get("/interno/gestion/presupuestos").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var match=Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(html);
        assertTrue(match.find());csrf=match.group(1);
    }
    MockHttpSession sesion(String rol){var s=new MockHttpSession();s.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(new UsernamePasswordAuthenticationToken("gestion-qa","",List.of(new SimpleGrantedAuthority("ROLE_"+rol)))));return s;}
    @Test void paginasYRedireccionesCarganConCsrf() throws Exception {
        for(String modulo:GestionInternaService.MODULOS){
            mvc.perform(get("/interno/modulos/"+modulo).session(session)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/interno/gestion/"+modulo));
            mvc.perform(get("/interno/gestion/"+modulo).session(session)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
            mvc.perform(get("/interno/gestion/"+modulo+"/datos").session(session)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }
    @Test void apiCreaApruebaYCobraSinModificarPortal() throws Exception {
        long paciente=dao.insertar("INSERT INTO pacientes(nombres,apellidos,fecha_nacimiento,celular,activo) VALUES('HTTP QA',?,'1990-01-01','999000000',1)",UUID.randomUUID().toString());
        var payload=new Presupuesto(paciente,"Presupuesto HTTP",LocalDate.now().plusDays(1),"",List.of(new Item("Concepto",1,new BigDecimal("100"))),0);
        String created=mvc.perform(post("/interno/gestion/presupuestos").session(session).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).get("id").asLong();
        mvc.perform(post("/interno/gestion/presupuestos/"+id+"/estado").session(session).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new Estado("APROBADO","",0)))).andExpect(status().isOk());
        var pago=new Pago(id,new BigDecimal("40"),LocalDate.now(),"EFECTIVO","",UUID.randomUUID().toString());
        mvc.perform(post("/interno/gestion/pagos").session(session).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(pago))).andExpect(status().isOk());
        mvc.perform(get("/interno/gestion/presupuestos/"+id).session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.saldo").value(60));
        mvc.perform(get("/interno/gestion/reportes/cobros.csv").session(session).param("paciente",String.valueOf(paciente))).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=\"dentia-cobros.csv\""));
    }
    @Test void seguridadRealBloqueaCsrfPacienteYRolSinPermisos() throws Exception {
        mvc.perform(post("/interno/gestion/pagos").session(session).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/interno/gestion/documentos/1/archivo")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/interno/gestion/documentos/1/archivo").session(sesion("PACIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/interno/gestion/pagos/datos").session(sesion("QA_SIN_PERMISOS"))).andExpect(status().isForbidden());
    }
}
