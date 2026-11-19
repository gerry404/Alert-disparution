package cm.alerte.disparition.stockage;

import cm.alerte.disparition.commun.ErreurMetier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Photos sur disque, dans app.uploads. Le format est determine par les premiers
 * octets du fichier et non par le Content-Type envoye par le navigateur
 * (falsifiable) : un .php renomme en .jpg est refuse.
 */
@Service
public class StockageService {

    private final Path racine;

    public StockageService(@Value("${app.uploads}") String racine) {
        this.racine = Path.of(racine).toAbsolutePath().normalize();
    }

    public Path racine() {
        return racine;
    }

    /** @return l'URL publique, ex. /uploads/3f2a....jpg */
    public String enregistrerImage(MultipartFile fichier) {
        try {
            String extension = detecterFormat(fichier);
            Files.createDirectories(racine);
            String nom = UUID.randomUUID() + extension;
            try (InputStream in = fichier.getInputStream()) {
                Files.copy(in, racine.resolve(nom), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + nom;
        } catch (IOException e) {
            throw new UncheckedIOException("Échec d'enregistrement de la photo", e);
        }
    }

    static String detecterFormat(MultipartFile fichier) throws IOException {
        byte[] t = new byte[12];
        int lus;
        try (InputStream in = fichier.getInputStream()) {
            lus = in.readNBytes(t, 0, t.length);
        }
        if (lus >= 3 && (t[0] & 0xFF) == 0xFF && (t[1] & 0xFF) == 0xD8 && (t[2] & 0xFF) == 0xFF) return ".jpg";
        if (lus >= 8 && (t[0] & 0xFF) == 0x89 && t[1] == 'P' && t[2] == 'N' && t[3] == 'G') return ".png";
        if (lus >= 12 && t[0] == 'R' && t[1] == 'I' && t[2] == 'F' && t[3] == 'F'
                && t[8] == 'W' && t[9] == 'E' && t[10] == 'B' && t[11] == 'P') return ".webp";
        throw ErreurMetier.invalide("FORMAT_IMAGE", "Image JPG, PNG ou WEBP uniquement");
    }
}
