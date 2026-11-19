package cm.alerte.disparition.rapprochement;

import cm.alerte.disparition.alerte.Notification;
import cm.alerte.disparition.alerte.NotificationService;
import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.dossier.Dossier;
import cm.alerte.disparition.dossier.DossierDtos.DossierPublic;
import cm.alerte.disparition.dossier.DossierRepository;
import cm.alerte.disparition.dossier.Evenements.DossierPublie;
import cm.alerte.disparition.dossier.TypeDossier;
import cm.alerte.disparition.rapprochement.MoteurRapprochement.Signalement;
import cm.alerte.disparition.utilisateur.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RapprochementService {

    private static final Logger log = LoggerFactory.getLogger(RapprochementService.class);
    private static final int FENETRE_JOURS = 60;

    public record CorrespondanceDto(Long id, int score, List<String> raisons, LocalDateTime creeLe,
                                    DossierPublic perdu, String declarantPerdu,
                                    DossierPublic trouve, String declarantTrouve) {
        static CorrespondanceDto de(Correspondance c) {
            return new CorrespondanceDto(c.getId(), c.getScore(), List.of(c.getRaisons().split(" ; ")), c.getCreeLe(),
                    DossierPublic.de(c.getObjetPerdu()), c.getObjetPerdu().getDeclarant().nomAffiche(),
                    DossierPublic.de(c.getObjetTrouve()), c.getObjetTrouve().getDeclarant().nomAffiche());
        }
    }

    private final DossierRepository dossiers;
    private final CorrespondanceRepository correspondances;
    private final NotificationService notifications;
    private final int seuil;

    public RapprochementService(DossierRepository dossiers, CorrespondanceRepository correspondances,
                                NotificationService notifications,
                                @Value("${app.rapprochement.seuil:50}") int seuil) {
        this.dossiers = dossiers;
        this.correspondances = correspondances;
        this.notifications = notifications;
        this.seuil = seuil;
    }

    /** Lance l'analyse des qu'un objet est publie. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surPublication(DossierPublie e) {
        dossiers.findById(e.dossierId()).filter(d -> d.getType().estObjet()).ifPresent(this::analyser);
    }

    void analyser(Dossier nouveau) {
        var candidats = dossiers.candidatsRapprochement(nouveau.getType().oppose(),
                nouveau.getQuartier().getVille().getId(),
                nouveau.getDateFaits().minusDays(FENETRE_JOURS), nouveau.getDateFaits().plusDays(FENETRE_JOURS));

        int trouvees = 0;
        for (Dossier autre : candidats) {
            Dossier perdu = nouveau.getType() == TypeDossier.OBJET_PERDU ? nouveau : autre;
            Dossier trouve = perdu == nouveau ? autre : nouveau;
            if (perdu.getDeclarant().getId().equals(trouve.getDeclarant().getId())) continue;
            if (correspondances.existsByObjetPerduIdAndObjetTrouveId(perdu.getId(), trouve.getId())) continue;

            var evaluation = MoteurRapprochement.evaluer(signalement(perdu), signalement(trouve));
            if (evaluation.isPresent() && evaluation.get().score() >= seuil) {
                correspondances.save(new Correspondance(perdu, trouve, evaluation.get().score(),
                        String.join(" ; ", evaluation.get().raisons())));
                trouvees++;
            }
        }
        if (trouvees > 0) log.info("{} correspondance(s) potentielle(s) pour {}", trouvees, nouveau.getReference());
    }

    @Transactional(readOnly = true)
    public List<CorrespondanceDto> aExaminer() {
        return correspondances.findByStatutOrderByScoreDescCreeLeAsc(Correspondance.Statut.A_EXAMINER)
                .stream().map(CorrespondanceDto::de).toList();
    }

    /**
     * Validation par un moderateur : les deux declarants sont prevenus,
     * mais les coordonnees ne sont PAS echangees automatiquement : c'est la moderation
     * qui organise la verification de propriete (5.9).
     */
    @Transactional
    public void trancher(Long id, boolean valider, Utilisateur moderateur) {
        Correspondance c = correspondances.findById(id).orElseThrow(() -> ErreurMetier.introuvable("Correspondance"));
        c.trancher(valider ? Correspondance.Statut.VALIDEE : Correspondance.Statut.ECARTEE, moderateur);
        if (!valider) return;

        notifications.notifier(c.getObjetPerdu().getDeclarant(), Notification.Type.CORRESPONDANCE,
                "Votre objet a peut-être été retrouvé",
                "Un objet trouvé ressemble à « " + c.getObjetPerdu().getTitre()
                        + " ». Un modérateur va vous contacter pour vérifier que c'est bien le vôtre.",
                "/d/" + c.getObjetTrouve().getSlug());
        notifications.notifier(c.getObjetTrouve().getDeclarant(), Notification.Type.CORRESPONDANCE,
                "Le propriétaire a peut-être été identifié",
                "Merci ! Un modérateur va vérifier la propriété avant toute remise de « "
                        + c.getObjetTrouve().getTitre() + " ».",
                "/moi");
    }

    private static Signalement signalement(Dossier d) {
        return new Signalement(
                d.getCategorie() == null ? null : d.getCategorie().getId(),
                d.getMarque(), d.getModele(), d.getCouleur(), d.getQuartier().getId(), d.getDateFaits(),
                String.join(" ", nonNul(d.getTitre()), nonNul(d.getSignesDistinctifs()), nonNul(d.getDescription())));
    }

    private static String nonNul(String s) {
        return s == null ? "" : s;
    }
}
