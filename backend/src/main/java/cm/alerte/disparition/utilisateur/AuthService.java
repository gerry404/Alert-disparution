package cm.alerte.disparition.utilisateur;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.commun.LimiteurDebit;
import cm.alerte.disparition.commun.Textes;
import cm.alerte.disparition.securite.SessionService;
import cm.alerte.disparition.utilisateur.UtilisateurDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encodeur;
    private final SessionService sessions;
    private final LimiteurDebit limiteur;
    // on compare quand meme un hash quand le numero n'existe pas,
    // pour ne pas reveler par le temps de reponse quels numeros ont un compte
    private final String hashFactice;
    private final int inscriptionsParHeure;
    private final int echecsConnexionMax;

    public AuthService(UtilisateurRepository utilisateurs, PasswordEncoder encodeur,
                       SessionService sessions, LimiteurDebit limiteur,
                       @Value("${app.limites.inscriptions-par-heure:30}") int inscriptionsParHeure,
                       @Value("${app.limites.echecs-connexion:8}") int echecsConnexionMax) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.sessions = sessions;
        this.limiteur = limiteur;
        this.hashFactice = encodeur.encode(java.util.UUID.randomUUID().toString());
        this.inscriptionsParHeure = inscriptionsParHeure;
        this.echecsConnexionMax = echecsConnexionMax;
    }

    @Transactional
    public SessionDto inscrire(InscriptionRequest req, String ip) {
        // limite large : au Cameroun beaucoup d'abonnes mobiles partagent la meme IP publique (CGNAT)
        limiteur.verifier("inscription:" + ip, inscriptionsParHeure, Duration.ofHours(1));
        String tel = Textes.telephone(req.telephone());
        if (utilisateurs.existsByTelephone(tel)) {
            throw ErreurMetier.conflit("TELEPHONE_DEJA_UTILISE", "Ce numéro a déjà un compte");
        }
        Utilisateur u = utilisateurs.save(new Utilisateur(req.nom().trim(), Textes.vide(req.prenom()), tel,
                Textes.vide(req.email()), encodeur.encode(req.motDePasse())));
        return new SessionDto(sessions.ouvrir(u), UtilisateurDto.de(u));
    }

    @Transactional
    public SessionDto connecter(ConnexionRequest req, String ip) {
        String tel = Textes.telephone(req.telephone());
        // seuls les ECHECS comptent : se connecter/deconnecter plusieurs fois ne bloque personne
        String cle = "connexion:" + ip + ":" + tel;
        limiteur.exigerNonBloque(cle, echecsConnexionMax, Duration.ofMinutes(15));

        var trouve = utilisateurs.findByTelephone(tel);
        boolean motDePasseOk = encodeur.matches(req.motDePasse(),
                trouve.map(Utilisateur::getMotDePasse).orElse(hashFactice));
        var u = trouve.filter(x -> motDePasseOk && x.isActif());
        if (u.isEmpty()) {
            limiteur.enregistrerEchec(cle);
            throw ErreurMetier.nonAuthentifie("Numéro ou mot de passe incorrect");
        }
        limiteur.effacer(cle);
        return new SessionDto(sessions.ouvrir(u.get()), UtilisateurDto.de(u.get()));
    }
}
