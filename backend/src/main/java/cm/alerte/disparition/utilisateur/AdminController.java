package cm.alerte.disparition.utilisateur;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.securite.SessionService;
import cm.alerte.disparition.utilisateur.UtilisateurDtos.ModificationCompteRequest;
import cm.alerte.disparition.utilisateur.UtilisateurDtos.UtilisateurAdminDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/utilisateurs")
class AdminController {

    private final UtilisateurRepository utilisateurs;
    private final SessionService sessions;

    AdminController(UtilisateurRepository utilisateurs, SessionService sessions) {
        this.utilisateurs = utilisateurs;
        this.sessions = sessions;
    }

    @GetMapping
    @Transactional(readOnly = true)
    PageDto<UtilisateurAdminDto> lister(@RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page) {
        String motif = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        var pageable = PageRequest.of(page, 20, Sort.by("creeLe").descending());
        return PageDto.de(utilisateurs.rechercher(motif, pageable), UtilisateurAdminDto::de);
    }

    @PatchMapping("/{id}")
    @Transactional
    UtilisateurAdminDto modifier(@PathVariable Long id, @RequestBody ModificationCompteRequest req,
                                 @AuthenticationPrincipal Utilisateur admin) {
        Utilisateur u = utilisateurs.findById(id).orElseThrow(() -> ErreurMetier.introuvable("Utilisateur"));
        if (u.getId().equals(admin.getId())) {
            throw ErreurMetier.interdit("Vous ne pouvez pas modifier votre propre compte ici");
        }
        if (req.role() != null) u.setRole(req.role());
        if (req.actif() != null) {
            u.setActif(req.actif());
            if (!req.actif()) sessions.fermerToutes(u); // deconnexion immediate
        }
        return UtilisateurAdminDto.de(u);
    }
}
