package cm.alerte.disparition.dossier;

import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.dossier.DossierDtos.*;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
class DossierController {

    private final DossierService service;

    DossierController(DossierService service) {
        this.service = service;
    }

    // ---- public

    @GetMapping("/api/dossiers")
    PageDto<DossierPublic> rechercher(@RequestParam(required = false) TypeDossier type,
                                      @RequestParam(required = false) Long quartierId,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean inclureResolus,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "12") int taille) {
        return service.rechercher(type, quartierId, q, inclureResolus, page, taille);
    }

    @GetMapping("/api/dossiers/{slug}")
    DossierPublic fiche(@PathVariable String slug) {
        return service.fichePublique(slug);
    }

    @GetMapping("/api/referentiel/stats")
    StatsDto stats() {
        return service.stats();
    }

    // ---- declarant connecte

    @GetMapping("/api/moi/dossiers")
    List<DossierPrive> mesDossiers(@AuthenticationPrincipal Utilisateur moi) {
        return service.mesDossiers(moi);
    }

    @PostMapping(value = "/api/moi/dossiers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    DossierPrive signaler(@Valid @ModelAttribute DossierForm form, @AuthenticationPrincipal Utilisateur moi) {
        return service.signaler(form, moi);
    }

    @PostMapping("/api/moi/dossiers/{id}/retrouve")
    DossierPrive declarerRetrouve(@PathVariable Long id, @RequestBody DecisionRequest req,
                                  @AuthenticationPrincipal Utilisateur moi) {
        return service.declarerRetrouve(id, req.motif(), moi);
    }

    // ---- moderation

    @GetMapping("/api/moderation/dossiers")
    List<DossierPrive> aModerer() {
        return service.aModerer();
    }

    @PostMapping("/api/moderation/dossiers/{id}/{decision}")
    DossierPrive decider(@PathVariable Long id, @PathVariable String decision,
                         @RequestBody(required = false) DecisionRequest req,
                         @AuthenticationPrincipal Utilisateur moderateur) {
        String motif = req == null ? null : req.motif();
        return switch (decision) {
            case "masquer" -> service.masquer(id, true, motif, moderateur);
            case "afficher" -> service.masquer(id, false, motif, moderateur);
            default -> service.decider(id, decisionDe(decision), motif, moderateur);
        };
    }

    private static DecisionModeration decisionDe(String texte) {
        try {
            return DecisionModeration.valueOf(texte.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw cm.alerte.disparition.commun.ErreurMetier.introuvable("Action « " + texte + " »");
        }
    }
}
