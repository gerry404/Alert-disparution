package cm.alerte.disparition.dossier;

import java.util.EnumSet;
import java.util.Set;

/**
 * Decisions qu'un moderateur peut prendre sur un dossier.
 * "depuis" restreint plus finement que la machine a etats : PUBLIER et ROUVRIR
 * menent tous deux a PUBLIE mais ne partent pas des memes statuts.
 */
public enum DecisionModeration {

    PUBLIER(StatutDossier.PUBLIE, EnumSet.of(StatutDossier.EN_ATTENTE), false),
    REJETER(StatutDossier.REJETE, EnumSet.of(StatutDossier.EN_ATTENTE), true),
    RESOUDRE(StatutDossier.RESOLU, EnumSet.of(StatutDossier.PUBLIE, StatutDossier.EN_VERIFICATION), true),
    ROUVRIR(StatutDossier.PUBLIE, EnumSet.of(StatutDossier.EN_VERIFICATION, StatutDossier.RESOLU), true);

    final StatutDossier cible;
    final Set<StatutDossier> depuis;
    final boolean motifObligatoire;

    DecisionModeration(StatutDossier cible, Set<StatutDossier> depuis, boolean motifObligatoire) {
        this.cible = cible;
        this.depuis = depuis;
        this.motifObligatoire = motifObligatoire;
    }
}
