package cm.alerte.disparition.controller;

import cm.alerte.disparition.dto.Dto.DossierPrive;
import cm.alerte.disparition.dto.Dto.MotifRequest;
import cm.alerte.disparition.dto.DossierForm;
import cm.alerte.disparition.model.Dossier;
import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Utilisateur;
import cm.alerte.disparition.repository.DossierRepository;
import cm.alerte.disparition.service.DossierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Espace du declarant connecte.
 */
@RestController
@RequestMapping("/api/moi/dossiers")
public class MesDossiersController {

    private final DossierRepository dossiers;
    private final DossierService service;

    public MesDossiersController(DossierRepository dossiers, DossierService service) {
        this.dossiers = dossiers;
        this.service = service;
    }

    @GetMapping
    public List<DossierPrive> mesDossiers(@AuthenticationPrincipal Utilisateur u) {
        return dossiers.findByDeclarantOrderByDateSoumissionDesc(u).stream().map(service::versPrive).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DossierPrive signaler(@Valid @ModelAttribute DossierForm form, @AuthenticationPrincipal Utilisateur u) {
        return service.versPrive(service.creer(form, u));
    }

    /** "La personne / l'objet a ete retrouve" : on passe en verification, jamais en resolu direct (RG10). */
    @PostMapping("/{id}/retrouve")
    public DossierPrive declarerRetrouve(@PathVariable Long id, @RequestBody MotifRequest req,
                                         @AuthenticationPrincipal Utilisateur u) {
        Dossier d = dossiers.findById(id)
                .filter(x -> x.getDeclarant().getId().equals(u.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (d.getStatut() != StatutDossier.PUBLIE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le dossier doit être publié");
        }
        service.changerStatut(d, StatutDossier.EN_VERIFICATION, u, req.motif());
        return service.versPrive(d);
    }
}
