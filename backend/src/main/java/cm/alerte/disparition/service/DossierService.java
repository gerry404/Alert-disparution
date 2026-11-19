package cm.alerte.disparition.service;

import cm.alerte.disparition.dto.Dto.DossierPrive;
import cm.alerte.disparition.dto.Dto.DossierPublic;
import cm.alerte.disparition.dto.Dto.HistoriqueDto;
import cm.alerte.disparition.dto.DossierForm;
import cm.alerte.disparition.model.*;
import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Enums.TypeDossier;
import cm.alerte.disparition.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.Year;

@Service
public class DossierService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final DossierRepository dossiers;
    private final QuartierRepository quartiers;
    private final CategorieRepository categories;
    private final HistoriqueStatutRepository historiques;
    private final StockageService stockage;

    public DossierService(DossierRepository dossiers, QuartierRepository quartiers, CategorieRepository categories,
                          HistoriqueStatutRepository historiques, StockageService stockage) {
        this.dossiers = dossiers;
        this.quartiers = quartiers;
        this.categories = categories;
        this.historiques = historiques;
        this.stockage = stockage;
    }

    @Transactional
    public Dossier creer(DossierForm f, Utilisateur declarant) {
        Dossier d = new Dossier();
        d.setType(f.getType());
        d.setTitre(f.getTitre().trim());
        d.setDescription(f.getDescription());
        d.setDateFaits(f.getDateFaits());
        d.setLieuPrecis(f.getLieuPrecis());
        d.setSignesDistinctifs(f.getSignesDistinctifs());
        d.setDeclarant(declarant);
        d.setQuartier(quartiers.findById(f.getQuartierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartier inconnu")));

        if (f.getType() == TypeDossier.PERSONNE) {
            d.setNomComplet(f.getNomComplet());
            d.setSexe(f.getSexe());
            d.setAge(f.getAge());
            d.setDescriptionPhysique(f.getDescriptionPhysique());
            d.setCirconstances(f.getCirconstances());
        } else {
            d.setMarque(f.getMarque());
            d.setModele(f.getModele());
            d.setCouleur(f.getCouleur());
            if (f.getCategorieId() != null) {
                d.setCategorie(categories.findById(f.getCategorieId()).orElse(null));
            }
        }

        if (f.getPhoto() != null && !f.getPhoto().isEmpty()) {
            d.setPhoto(stockage.enregistrerImage(f.getPhoto()));
        }

        d.setReference(nouvelleReference());
        d.setSlug(slugifier(d.getTitre()) + "-" + code(5).toLowerCase());
        dossiers.save(d);
        tracer(d, null, StatutDossier.EN_ATTENTE, declarant, "Signalement soumis");
        return d;
    }

    /** Tout changement de statut passe par ici pour garder l'historique (RG12). */
    @Transactional
    public void changerStatut(Dossier d, StatutDossier nouveau, Utilisateur auteur, String motif) {
        StatutDossier ancien = d.getStatut();
        d.setStatut(nouveau);
        if (nouveau == StatutDossier.PUBLIE && d.getDatePublication() == null) {
            d.setDatePublication(LocalDateTime.now());
        }
        dossiers.save(d);
        tracer(d, ancien, nouveau, auteur, motif);
    }

    public DossierPrive versPrive(Dossier d) {
        return new DossierPrive(
                DossierPublic.de(d), d.getLieuPrecis(), d.isMasque(), d.estMineur(),
                d.getDeclarant().getNomAffiche(), d.getDeclarant().getTelephone(), d.getDateSoumission(),
                historiques.findByDossierOrderByDateAsc(d).stream().map(HistoriqueDto::de).toList());
    }

    private void tracer(Dossier d, StatutDossier ancien, StatutDossier nouveau, Utilisateur auteur, String motif) {
        HistoriqueStatut h = new HistoriqueStatut();
        h.setDossier(d);
        h.setAncienStatut(ancien);
        h.setNouveauStatut(nouveau);
        h.setAuteur(auteur);
        h.setMotif(motif);
        historiques.save(h);
    }

    private String nouvelleReference() {
        return "AD-" + Year.now().getValue() + "-" + code(5);
    }

    private String code(int longueur) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longueur; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    static String slugifier(String texte) {
        String s = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
