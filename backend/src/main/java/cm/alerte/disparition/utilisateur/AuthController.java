package cm.alerte.disparition.utilisateur;

import cm.alerte.disparition.securite.SessionService;
import cm.alerte.disparition.utilisateur.UtilisateurDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService auth;
    private final SessionService sessions;

    AuthController(AuthService auth, SessionService sessions) {
        this.auth = auth;
        this.sessions = sessions;
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    SessionDto inscrire(@Valid @RequestBody InscriptionRequest req, HttpServletRequest http) {
        return auth.inscrire(req, http.getRemoteAddr());
    }

    @PostMapping("/connexion")
    SessionDto connecter(@Valid @RequestBody ConnexionRequest req, HttpServletRequest http) {
        return auth.connecter(req, http.getRemoteAddr());
    }

    @GetMapping("/moi")
    UtilisateurDto moi(@AuthenticationPrincipal Utilisateur u) {
        return UtilisateurDto.de(u);
    }

    @PostMapping("/deconnexion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deconnecter() {
        Object token = SecurityContextHolder.getContext().getAuthentication().getCredentials();
        if (token instanceof String t) sessions.fermer(t);
    }
}
