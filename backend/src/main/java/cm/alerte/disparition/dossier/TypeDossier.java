package cm.alerte.disparition.dossier;

public enum TypeDossier {
    PERSONNE,
    OBJET_PERDU,
    OBJET_TROUVE;

    public boolean estObjet() {
        return this != PERSONNE;
    }

    /** Perdu <-> trouve, pour le rapprochement. */
    public TypeDossier oppose() {
        return switch (this) {
            case OBJET_PERDU -> OBJET_TROUVE;
            case OBJET_TROUVE -> OBJET_PERDU;
            case PERSONNE -> throw new IllegalStateException("Pas de rapprochement pour une personne");
        };
    }
}
