package cm.alerte.disparition.alerte;

import cm.alerte.disparition.dossier.Dossier;
import cm.alerte.disparition.dossier.DossierRepository;
import cm.alerte.disparition.dossier.Evenements.DossierPublie;
import cm.alerte.disparition.dossier.Evenements.StatutDossierChange;
import cm.alerte.disparition.dossier.StatutDossier;
import cm.alerte.disparition.dossier.TypeDossier;
import cm.alerte.disparition.temoignage.TemoignageDtos.TemoignagePublie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Traduit les evenements metier en notifications.
 * Execute apres le commit, dans sa propre transaction.
 */
@Component
class DiffusionAlertes {

    private static final Logger log = LoggerFactory.getLogger(DiffusionAlertes.class);

    private final DossierRepository dossiers;
    private final AbonnementRepository abonnements;
    private final NotificationService notifications;

    DiffusionAlertes(DossierRepository dossiers, AbonnementRepository abonnements, NotificationService notifications) {
        this.dossiers = dossiers;
        this.abonnements = abonnements;
        this.notifications = notifications;
    }

    /**
     * RG08 / CA08 : seule la publication d'un NOUVEAU dossier de disparition, visible
     * publiquement, declenche l'alerte. Pas les objets, pas les mineurs, pas les temoignages.
     */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surPublication(DossierPublie e) {
        Dossier d = dossiers.findById(e.dossierId()).orElse(null);
        if (d == null || d.getType() != TypeDossier.PERSONNE || !d.estVisiblePubliquement()) return;

        var abonnes = abonnements.actifsPourQuartier(d.getQuartier().getId());
        abonnes.stream()
                .map(AbonnementAlerte::getUtilisateur)
                .filter(u -> !d.appartientA(u))
                .forEach(u -> notifications.notifier(u, Notification.Type.NOUVELLE_DISPARITION,
                        "Disparition signalée à " + d.getQuartier().getNom(),
                        d.getTitre() + ". Ouvrez l'avis pour voir la description et partager.",
                        "/d/" + d.getSlug()));
        log.info("Alerte zone {} : {} abonné(s) prévenu(s) pour {}", d.getQuartier().getNom(), abonnes.size(), d.getReference());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surDecision(StatutDossierChange e) {
        dossiers.findById(e.dossierId()).ifPresent(d -> {
            String message = switch (e.nouveau()) {
                case PUBLIE -> e.ancien() == StatutDossier.EN_ATTENTE
                        ? "Votre avis est en ligne. Partagez le lien autour de vous."
                        : "Votre avis est de nouveau en ligne : " + e.motif();
                case REJETE -> "Votre signalement n'a pas été publié : " + e.motif();
                case RESOLU -> "Le dossier est clôturé : " + e.motif();
                default -> null;
            };
            if (message != null) {
                String lien = d.estVisiblePubliquement() ? "/d/" + d.getSlug() : "/moi";
                notifications.notifier(d.getDeclarant(), Notification.Type.STATUT_DOSSIER, d.getTitre(), message, lien);
            }
        });
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surTemoignage(TemoignagePublie e) {
        dossiers.findById(e.dossierId()).ifPresent(d -> notifications.notifier(d.getDeclarant(),
                Notification.Type.TEMOIGNAGE_PUBLIE, "Nouveau témoignage",
                "Quelqu'un a transmis une information sur « " + d.getTitre() + " ».", "/d/" + d.getSlug()));
    }
}
