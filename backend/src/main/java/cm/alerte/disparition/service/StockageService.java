package cm.alerte.disparition.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Stocke les photos sur le disque (dossier "uploads").
 * Plus tard on pourra brancher un stockage externe sans toucher au reste.
 */
@Service
public class StockageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path dossier;

    public StockageService(@Value("${app.uploads}") String dossier) {
        this.dossier = Path.of(dossier).toAbsolutePath();
    }

    /** Renvoie l'URL publique du fichier, ex : /uploads/3f2a...jpg */
    public String enregistrerImage(MultipartFile fichier) {
        String ext = EXTENSIONS.get(fichier.getContentType());
        if (ext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image JPG, PNG ou WEBP uniquement");
        }
        try {
            Files.createDirectories(dossier);
            String nom = UUID.randomUUID() + ext;
            fichier.transferTo(dossier.resolve(nom));
            return "/uploads/" + nom;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer la photo");
        }
    }
}
