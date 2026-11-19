package cm.alerte.disparition.dossier;

/**
 * Evenements publies par le domaine. Les modules alerte et rapprochement
 * les ecoutent APRES le commit : un echec d'envoi de notification
 * n'annule jamais une decision de moderation.
 */
public final class Evenements {

    private Evenements() {
    }

    /** Premiere publication d'un dossier (les republications apres reouverture ne comptent pas). */
    public record DossierPublie(Long dossierId) {
    }

    /** Toute decision qui interesse le declarant. */
    public record StatutDossierChange(Long dossierId, StatutDossier ancien, StatutDossier nouveau, String motif) {
    }
}
