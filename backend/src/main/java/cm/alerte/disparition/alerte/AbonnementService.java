package cm.alerte.disparition.alerte;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.referentiel.QuartierRepository;
import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AbonnementService {

    public record AbonnementDto(Long id, Long quartierId, String quartier, String arrondissement, boolean actif) {
        static AbonnementDto de(AbonnementAlerte a) {
            return new AbonnementDto(a.getId(), a.getQuartier().getId(), a.getQuartier().getNom(),
                    a.getQuartier().getArrondissement(), a.isActif());
        }
    }

    static final int MAX_ZONES = 10;

    private final AbonnementRepository abonnements;
    private final QuartierRepository quartiers;

    public AbonnementService(AbonnementRepository abonnements, QuartierRepository quartiers) {
        this.abonnements = abonnements;
        this.quartiers = quartiers;
    }

    @Transactional(readOnly = true)
    public List<AbonnementDto> mesZones(Utilisateur u) {
        return abonnements.findByUtilisateurOrderByCreeLeAsc(u).stream().map(AbonnementDto::de).toList();
    }

    /** Idempotent : se reabonner a une zone desactivee la reactive. */
    @Transactional
    public AbonnementDto ajouter(Utilisateur u, Long quartierId) {
        var existant = abonnements.findByUtilisateurAndQuartierId(u, quartierId);
        if (existant.isPresent()) {
            existant.get().setActif(true);
            return AbonnementDto.de(existant.get());
        }
        if (abonnements.findByUtilisateurOrderByCreeLeAsc(u).size() >= MAX_ZONES) {
            throw ErreurMetier.invalide("TROP_DE_ZONES", "Vous suivez déjà " + MAX_ZONES + " quartiers");
        }
        var quartier = quartiers.findById(quartierId)
                .orElseThrow(() -> ErreurMetier.invalide("QUARTIER_INCONNU", "Quartier inconnu"));
        return AbonnementDto.de(abonnements.save(new AbonnementAlerte(u, quartier)));
    }

    @Transactional
    public AbonnementDto activer(Utilisateur u, Long id, boolean actif) {
        AbonnementAlerte a = chargerLeMien(u, id);
        a.setActif(actif);
        return AbonnementDto.de(a);
    }

    @Transactional
    public void supprimer(Utilisateur u, Long id) {
        abonnements.delete(chargerLeMien(u, id));
    }

    private AbonnementAlerte chargerLeMien(Utilisateur u, Long id) {
        return abonnements.findById(id)
                .filter(a -> a.getUtilisateur().getId().equals(u.getId()))
                .orElseThrow(() -> ErreurMetier.introuvable("Abonnement"));
    }
}
