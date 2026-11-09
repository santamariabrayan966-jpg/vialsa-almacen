package com.dentia.controller.paciente;

import com.dentia.security.GoogleOAuthAvailability;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/paciente")
public class PatientAuthController {

    private final GoogleOAuthAvailability googleOAuthAvailability;

    public PatientAuthController(GoogleOAuthAvailability googleOAuthAvailability) {
        this.googleOAuthAvailability = googleOAuthAvailability;
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_PACIENTE".equals(a.getAuthority()))) {
            return "redirect:/paciente";
        }
        return "redirect:/?loginGoogle";
    }

    /** Enlaces antiguos pasan por OAuth real; esta ruta no crea cuentas ni sesiones. */
    @GetMapping("/login-google-demo")
    public String loginGoogleDemo() {
        return googleOAuthAvailability.isConfigured()
                ? "redirect:/oauth2/authorization/google"
                : "redirect:/?oauthNoDisponible";
    }

    @GetMapping("/registro")
    public String registro() {
        return "redirect:/?soloGoogle";
    }

    @PostMapping("/registro")
    public String registrar() {
        return "redirect:/?soloGoogle";
    }

}
