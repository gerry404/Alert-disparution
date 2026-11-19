package cm.alerte.disparition.alerte;

import cm.alerte.disparition.alerte.AbonnementService.AbonnementDto;
import cm.alerte.disparition.alerte.NotificationService.NotificationDto;
import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moi")
class AlerteController {

    record ZoneRequest(@NotNull Long quartierId) {
    }

    record ActivationRequest(boolean actif) {
    }

    private final AbonnementService abonnements;
    private final NotificationService notifications;

    AlerteController(AbonnementService abonnements, NotificationService notifications) {
        this.abonnements = abonnements;
        this.notifications = notifications;
    }

    // ---- "Mes alertes" (5.8)

    @GetMapping("/alertes")
    List<AbonnementDto> mesZones(@AuthenticationPrincipal Utilisateur moi) {
        return abonnements.mesZones(moi);
    }

    @PostMapping("/alertes")
    @ResponseStatus(HttpStatus.CREATED)
    AbonnementDto ajouter(@RequestBody @jakarta.validation.Valid ZoneRequest req, @AuthenticationPrincipal Utilisateur moi) {
        return abonnements.ajouter(moi, req.quartierId());
    }

    @PatchMapping("/alertes/{id}")
    AbonnementDto activer(@PathVariable Long id, @RequestBody ActivationRequest req,
                          @AuthenticationPrincipal Utilisateur moi) {
        return abonnements.activer(moi, id, req.actif());
    }

    @DeleteMapping("/alertes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void supprimer(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moi) {
        abonnements.supprimer(moi, id);
    }

    // ---- notifications

    @GetMapping("/notifications")
    PageDto<NotificationDto> notifications(@RequestParam(defaultValue = "0") int page,
                                           @AuthenticationPrincipal Utilisateur moi) {
        return notifications.lister(moi, page);
    }

    @GetMapping("/notifications/non-lues")
    Map<String, Long> nonLues(@AuthenticationPrincipal Utilisateur moi) {
        return Map.of("total", notifications.nonLues(moi));
    }

    @PostMapping("/notifications/{id}/lue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void marquerLue(@PathVariable Long id, @AuthenticationPrincipal Utilisateur moi) {
        notifications.marquerLue(id, moi);
    }

    @PostMapping("/notifications/lues")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void toutMarquerLu(@AuthenticationPrincipal Utilisateur moi) {
        notifications.toutMarquerLu(moi);
    }
}
