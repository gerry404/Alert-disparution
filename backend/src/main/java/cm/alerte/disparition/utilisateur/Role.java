package cm.alerte.disparition.utilisateur;

public enum Role {
    DECLARANT,
    MODERATEUR,
    ADMIN;

    public boolean peutModerer() {
        return this == MODERATEUR || this == ADMIN;
    }
}
