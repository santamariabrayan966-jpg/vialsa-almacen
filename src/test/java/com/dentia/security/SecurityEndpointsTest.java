package com.dentia.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertFalse;

@WebMvcTest(controllers = SecurityEndpointsTest.Probe.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class))
@Import({FallbackSecurityConfig.class, SecurityResponseHeadersFilter.class})
class SecurityEndpointsTest {
    @Autowired MockMvc mvc;
    @Autowired SecurityFilterChain fallbackFilterChain;

    @Test
    void fallbackNoBloqueaElLoginNiLaPortada() {
        MockHttpServletRequest login = new MockHttpServletRequest("GET", "/interno/login");
        login.setServletPath("/interno/login");
        MockHttpServletRequest portada = new MockHttpServletRequest("GET", "/");
        portada.setServletPath("/");
        assertFalse(fallbackFilterChain.matches(login));
        assertFalse(fallbackFilterChain.matches(portada));
    }

    @Test
    void rutaNuevaNoQuedaPublicaPorAccidente() throws Exception {
        mvc.perform(get("/private-test"))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @RestController
    static class Probe {
        @GetMapping("/private-test")
        String privateData() { return "sensible"; }
    }
}
