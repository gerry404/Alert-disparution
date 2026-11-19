package cm.alerte.disparition.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renvoie toujours un JSON du type { "message": "...", "erreurs": { champ: message } }
 * pour que le front puisse afficher les erreurs sous les champs.
 */
@RestControllerAdvice
public class GestionErreurs {

    // MethodArgumentNotValidException herite de BindException
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> validation(BindException e) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        e.getFieldErrors().forEach(f -> erreurs.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Certains champs sont invalides",
                "erreurs", erreurs));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> statut(ResponseStatusException e) {
        String message = e.getReason() != null ? e.getReason() : HttpStatus.valueOf(e.getStatusCode().value()).getReasonPhrase();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", message));
    }
}
