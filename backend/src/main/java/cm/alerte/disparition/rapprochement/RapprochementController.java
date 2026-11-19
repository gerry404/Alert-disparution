package cm.alerte.disparition.rapprochement;

import cm.alerte.disparition.rapprochement.RapprochementService.CorrespondanceDto;
import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderation/correspondances")
class RapprochementController {

    private final RapprochementService service;

    RapprochementController(RapprochementService service) {
        this.service = service;
    }

    @GetMapping
    List<CorrespondanceDto> aExaminer() {
        return service.aExaminer();
    }

    @PostMapping("/{id}/valider")
    void valider(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moderateur) {
        service.trancher(id, true, moderateur);
    }

    @PostMapping("/{id}/ecarter")
    void ecarter(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moderateur) {
        service.trancher(id, false, moderateur);
    }
}
