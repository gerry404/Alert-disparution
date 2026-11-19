package cm.alerte.disparition.dossier;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'un dossier (section 6.1 du cahier des charges).
 *
 * <pre>
 *  EN_ATTENTE --publier--> PUBLIE --retrouve--> EN_VERIFICATION --confirmer--> RESOLU
 *      |                     |  ^                    |                           |
 *    rejeter                 |  +----non confirme----+                           |
 *      v                     +------------resolution directe------------------->|
 *   REJETE                   ^------------------------reouverture---------------+
 * </pre>
 */
public enum StatutDossier {
    EN_ATTENTE,
    PUBLIE,
    EN_VERIFICATION,
    RESOLU,
    REJETE;

    public Set<StatutDossier> suivants() {
        return switch (this) {
            case EN_ATTENTE -> EnumSet.of(PUBLIE, REJETE);
            case PUBLIE -> EnumSet.of(EN_VERIFICATION, RESOLU);
            case EN_VERIFICATION -> EnumSet.of(RESOLU, PUBLIE);
            case RESOLU -> EnumSet.of(PUBLIE);
            case REJETE -> EnumSet.noneOf(StatutDossier.class);
        };
    }

    public boolean peutPasserA(StatutDossier cible) {
        return suivants().contains(cible);
    }

    /** Statuts qu'un visiteur peut voir (les dossiers en attente et rejetes restent caches). */
    public boolean estPublic() {
        return this == PUBLIE || this == EN_VERIFICATION || this == RESOLU;
    }

    public String libelle() {
        return switch (this) {
            case EN_ATTENTE -> "en attente";
            case PUBLIE -> "publié";
            case EN_VERIFICATION -> "en vérification";
            case RESOLU -> "résolu";
            case REJETE -> "rejeté";
        };
    }
}
