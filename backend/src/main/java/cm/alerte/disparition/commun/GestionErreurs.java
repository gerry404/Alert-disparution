package cm.alerte.disparition.commun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Toutes les erreurs sortent au format ProblemDetail :
 * { "status": 409, "title": "...", "detail": "...", "code": "TRANSITION_INTERDITE", "erreurs": {...} }
 */
@RestControllerAdvice
public class GestionErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionErreurs.class);

    @ExceptionHandler(ErreurMetier.class)
    ProblemDetail metier(ErreurMetier e) {
        return probleme(e.statut(), e.code(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    ProblemDetail validation(BindException e) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        e.getFieldErrors().forEach(f -> erreurs.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail p = probleme(HttpStatus.BAD_REQUEST, "VALIDATION", "Certains champs sont invalides");
        p.setProperty("erreurs", erreurs);
        return p;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeIncorrect(MethodArgumentTypeMismatchException e) {
        return probleme(HttpStatus.BAD_REQUEST, "PARAMETRE_INVALIDE", "Paramètre invalide : " + e.getName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail fichierTropGros(MaxUploadSizeExceededException e) {
        return probleme(HttpStatus.CONTENT_TOO_LARGE, "FICHIER_TROP_GROS", "La photo dépasse la taille autorisée (5 Mo)");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrence(ObjectOptimisticLockingFailureException e) {
        log.info("Conflit de version : {}", e.getMessage());
        return probleme(HttpStatus.CONFLICT, "MODIFIE_ENTRE_TEMPS",
                "Ce dossier vient d'être modifié par quelqu'un d'autre. Rechargez la page.");
    }

    private static ProblemDetail probleme(HttpStatus statut, String code, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(statut, detail);
        p.setTitle(statut.getReasonPhrase());
        p.setProperty("code", code);
        return p;
    }
}
