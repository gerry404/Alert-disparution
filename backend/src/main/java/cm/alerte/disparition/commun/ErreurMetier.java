package cm.alerte.disparition.commun;

import org.springframework.http.HttpStatus;

/**
 * Erreur "attendue" : violation d'une regle de gestion, ressource absente, etc.
 * Traduite en ProblemDetail (RFC 9457) par {@link GestionErreurs}.
 */
public class ErreurMetier extends RuntimeException {

    private final HttpStatus statut;
    private final String code;

    public ErreurMetier(HttpStatus statut, String code, String message) {
        super(message);
        this.statut = statut;
        this.code = code;
    }

    public HttpStatus statut() {
        return statut;
    }

    public String code() {
        return code;
    }

    public static ErreurMetier introuvable(String ressource) {
        return new ErreurMetier(HttpStatus.NOT_FOUND, "INTROUVABLE", ressource + " introuvable");
    }

    public static ErreurMetier invalide(String code, String message) {
        return new ErreurMetier(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ErreurMetier conflit(String code, String message) {
        return new ErreurMetier(HttpStatus.CONFLICT, code, message);
    }

    public static ErreurMetier interdit(String message) {
        return new ErreurMetier(HttpStatus.FORBIDDEN, "INTERDIT", message);
    }

    public static ErreurMetier nonAuthentifie(String message) {
        return new ErreurMetier(HttpStatus.UNAUTHORIZED, "NON_AUTHENTIFIE", message);
    }

    public static ErreurMetier tropDeRequetes() {
        return new ErreurMetier(HttpStatus.TOO_MANY_REQUESTS, "TROP_DE_REQUETES",
                "Trop de tentatives. Réessayez dans quelques minutes.");
    }

    /** Raccourci pour les motifs obligatoires (RG11, rejet, acces identite...). */
    public static String exigerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw invalide("MOTIF_OBLIGATOIRE", "Un motif est obligatoire pour cette action");
        }
        return motif.trim();
    }
}
