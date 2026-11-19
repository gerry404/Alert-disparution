package cm.alerte.disparition.controller;

import cm.alerte.disparition.dto.Dto.*;
import cm.alerte.disparition.model.Utilisateur;
import cm.alerte.disparition.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encoder;

    public AuthController(UtilisateurRepository utilisateurs, PasswordEncoder encoder) {
        this.utilisateurs = utilisateurs;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDto inscrire(@Valid @RequestBody InscriptionRequest req) {
        String tel = normaliser(req.telephone());
        if (utilisateurs.existsByTelephone(tel)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro a déjà un compte");
        }
        Utilisateur u = new Utilisateur();
        u.setNom(req.nom().trim());
        u.setPrenom(req.prenom());
        u.setTelephone(tel);
        u.setEmail(req.email() == null || req.email().isBlank() ? null : req.email().trim());
        u.setMotDePasse(encoder.encode(req.motDePasse()));
        return ouvrirSession(u);
    }

    @PostMapping("/login")
    public SessionDto connecter(@Valid @RequestBody ConnexionRequest req) {
        Utilisateur u = utilisateurs.findByTelephone(normaliser(req.telephone()))
                .filter(x -> x.isActif() && encoder.matches(req.motDePasse(), x.getMotDePasse()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Numéro ou mot de passe incorrect"));
        return ouvrirSession(u);
    }

    @GetMapping("/me")
    public UtilisateurDto moi(@AuthenticationPrincipal Utilisateur u) {
        return UtilisateurDto.de(u);
    }

    @PostMapping("/logout")
    public void deconnecter(@AuthenticationPrincipal Utilisateur u) {
        u.setToken(null);
        utilisateurs.save(u);
    }

    private SessionDto ouvrirSession(Utilisateur u) {
        u.setToken(UUID.randomUUID().toString());
        utilisateurs.save(u);
        return new SessionDto(u.getToken(), UtilisateurDto.de(u));
    }

    /** "6 99 11 22 33" et "+237699112233" doivent donner le meme compte. */
    static String normaliser(String tel) {
        String t = tel.replace(" ", "");
        if (t.startsWith("+237")) t = t.substring(4);
        return t;
    }
}
