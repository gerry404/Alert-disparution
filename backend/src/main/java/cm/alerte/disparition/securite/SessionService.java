package cm.alerte.disparition.securite;

import cm.alerte.disparition.utilisateur.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessions;
    private final Clock horloge;
    private final Duration duree;
    private final SecureRandom aleatoire = new SecureRandom();

    public SessionService(SessionRepository sessions, Clock horloge,
                          @Value("${app.securite.duree-session:P30D}") Duration duree) {
        this.sessions = sessions;
        this.horloge = horloge;
        this.duree = duree;
    }

    /** Cree une session et renvoie le token en clair (il ne sera plus jamais lisible ensuite). */
    @Transactional
    public String ouvrir(Utilisateur utilisateur) {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        sessions.save(new Session(utilisateur, empreinte(token), maintenant, maintenant.plus(duree)));
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<Utilisateur> utilisateurDe(String token) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        return sessions.findByEmpreinteToken(empreinte(token))
                .filter(s -> s.estValide(maintenant))
                .map(Session::getUtilisateur);
    }

    @Transactional
    public void fermer(String token) {
        sessions.supprimerParEmpreinte(empreinte(token));
    }

    @Transactional
    public void fermerToutes(Utilisateur utilisateur) {
        sessions.supprimerToutes(utilisateur);
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purger() {
        int n = sessions.supprimerExpirees(LocalDateTime.now(horloge));
        if (n > 0) log.info("{} session(s) expirée(s) supprimée(s)", n);
    }

    static String empreinte(String token) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
