package cm.alerte.disparition.temoignage;

import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.temoignage.TemoignageDtos.*;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
class TemoignageController {

    private final TemoignageService service;

    TemoignageController(TemoignageService service) {
        this.service = service;
    }

    @PostMapping("/api/dossiers/{dossierId}/temoignages")
    @ResponseStatus(HttpStatus.ACCEPTED) // accepte, mais publie seulement apres moderation
    void soumettre(@PathVariable Long dossierId, @Valid @RequestBody TemoignageRequest req, HttpServletRequest http) {
        service.soumettre(dossierId, req, http.getRemoteAddr());
    }

    @GetMapping("/api/dossiers/{dossierId}/temoignages")
    List<TemoignagePublic> publies(@PathVariable Long dossierId) {
        return service.publies(dossierId);
    }

    @GetMapping("/api/moderation/temoignages")
    List<TemoignageModeration> enAttente() {
        return service.enAttente();
    }

    @PostMapping("/api/moderation/temoignages/{id}/publier")
    void publier(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moderateur) {
        service.traiter(id, Temoignage.Statut.PUBLIE, moderateur);
    }

    @PostMapping("/api/moderation/temoignages/{id}/rejeter")
    void rejeter(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moderateur) {
        service.traiter(id, Temoignage.Statut.REJETE, moderateur);
    }

    @PostMapping("/api/moderation/temoignages/{id}/masquer")
    void masquer(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moderateur) {
        service.traiter(id, Temoignage.Statut.MASQUE, moderateur);
    }

    @PostMapping("/api/moderation/temoignages/{id}/identite")
    IdentiteTemoin identite(@PathVariable Long id, @RequestBody MotifRequest req,
                            @AuthenticationPrincipal Utilisateur moderateur) {
        return service.consulterIdentite(id, req.motif(), moderateur);
    }

    @GetMapping("/api/admin/journal-acces")
    PageDto<AccesIdentiteDto> journal(@RequestParam(defaultValue = "0") int page) {
        return service.journal(page);
    }
}
