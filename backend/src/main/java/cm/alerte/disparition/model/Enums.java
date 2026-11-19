package cm.alerte.disparition.model;

/**
 * Les enums du domaine, regroupees ici pour ne pas avoir 5 petits fichiers.
 */
public final class Enums {

    private Enums() {
    }

    public enum Role { DECLARANT, MODERATEUR, ADMIN }

    public enum TypeDossier { PERSONNE, OBJET_PERDU, OBJET_TROUVE }

    public enum StatutDossier { EN_ATTENTE, PUBLIE, EN_VERIFICATION, RESOLU, REJETE }

    public enum TypeTemoignage { APERCU, INFORMATION }

    public enum StatutTemoignage { EN_ATTENTE, PUBLIE, REJETE, MASQUE }
}
