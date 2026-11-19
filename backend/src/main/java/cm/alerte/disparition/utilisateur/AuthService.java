package cm.alerte.disparition.utilisateur;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.commun.LimiteurDebit;
import cm.alerte.disparition.commun.Textes;
import cm.alerte.disparition.securite.SessionService;
import cm.alerte.disparition.utilisateur.UtilisateurDtos.*;
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

    public AuthService(UtilisateurRepository utilisateurs, PasswordEncoder encodeur,
                       SessionService sessions, LimiteurDebit limiteur) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.sessions = sessions;
        this.limiteur = limiteur;
        this.hashFactice = encodeur.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional
    public SessionDto inscrire(InscriptionRequest req, String ip) {
        limiteur.verifier("inscription:" + ip, 5, Duration.ofHours(1));
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
        limiteur.verifier("connexion:" + ip + ":" + tel, 8, Duration.ofMinutes(15));

        var trouve = utilisateurs.findByTelephone(tel);
        boolean motDePasseOk = encodeur.matches(req.motDePasse(),
                trouve.map(Utilisateur::getMotDePasse).orElse(hashFactice));
        Utilisateur u = trouve.filter(x -> motDePasseOk && x.isActif())
                .orElseThrow(() -> ErreurMetier.nonAuthentifie("Numéro ou mot de passe incorrect"));
        return new SessionDto(sessions.ouvrir(u), UtilisateurDto.de(u));
    }
}
