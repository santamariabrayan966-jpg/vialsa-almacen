package com.dentia.controller.interno;

import com.dentia.model.interno.Usuario;
import com.dentia.service.interno.UsuarioService;
import com.dentia.service.shared.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PerfilTemplateTest {
    @Test
    void renderizaPerfilYEdicionConPermisos() throws Exception {
        UsuarioService service = mock(UsuarioService.class);
        Usuario usuario = new Usuario();
        usuario.setNombres("Prueba");
        usuario.setApellidos("Usuario");
        usuario.setNombreUsuario("prueba");
        when(service.obtenerUsuarioActual()).thenReturn(usuario);
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        var mvc = MockMvcBuilders.standaloneSetup(new PerfilInternoController(service, mock(FileStorageService.class)))
                .setControllerAdvice(new Advice()).setViewResolvers(views).build();
        mvc.perform(get("/interno/perfil")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Roles y permisos")));
        mvc.perform(get("/interno/perfil").param("editar", "1")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("perfilNuevaContrasena")));
    }
    @ControllerAdvice
    static class Advice {
        @ModelAttribute void header(Model model) {
            model.addAttribute("permisosUsuario", Map.of("usuarios", true, "roles", true, "configuracion", true));
            model.addAttribute("logoPrincipalUrl", "");
            model.addAttribute("_csrf", new Csrf());
        }
    }
    static class Csrf {
        public String getParameterName() { return "_csrf"; }
        public String getToken() { return "test-token"; }
    }
}
