package cm.alerte.disparition.dossier;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.commun.Textes;
import cm.alerte.disparition.dossier.DossierDtos.*;
import cm.alerte.disparition.dossier.Evenements.DossierPublie;
import cm.alerte.disparition.dossier.Evenements.StatutDossierChange;
import cm.alerte.disparition.referentiel.CategorieRepository;
import cm.alerte.disparition.referentiel.QuartierRepository;
import cm.alerte.disparition.stockage.StockageService;
import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Year;
import java.util.List;

@Service
public class DossierService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sans 0/O ni 1/I
    private final SecureRandom aleatoire = new SecureRandom();

    private final DossierRepository dossiers;
    private final QuartierRepository quartiers;
    private final CategorieRepository categories;
    private final StockageService stockage;
    private final ApplicationEventPublisher evenements;

    public DossierService(DossierRepository dossiers, QuartierRepository quartiers, CategorieRepository categories,
                          StockageService stockage, ApplicationEventPublisher evenements) {
        this.dossiers = dossiers;
        this.quartiers = quartiers;
        this.categories = categories;
        this.stockage = stockage;
        this.evenements = evenements;
    }

    // ------------------------------------------------------------------ public

    @Transactional(readOnly = true)
    public PageDto<DossierPublic> rechercher(TypeDossier type, Long quartierId, String q, boolean inclureResolus,
                                             int page, int taille) {
        Specification<Dossier> spec = DossierRepository.Filtres.visibles(inclureResolus);
        if (type != null) spec = spec.and(DossierRepository.Filtres.deType(type));
        if (quartierId != null) spec = spec.and(DossierRepository.Filtres.dansQuartier(quartierId));
        if (q != null && !q.isBlank()) spec = spec.and(DossierRepository.Filtres.contient(q.trim()));

        var pageable = PageRequest.of(Math.max(page, 0), PageDto.borner(taille),
                Sort.by(Sort.Order.desc("datePublication"), Sort.Order.desc("id")));
        return PageDto.de(dossiers.findAll(spec, pageable), DossierPublic::de);
    }

    @Transactional(readOnly = true)
    public DossierPublic fichePublique(String slug) {
        return dossiers.findBySlug(slug)
                .filter(Dossier::estVisiblePubliquement)
                .map(DossierPublic::de)
                .orElseThrow(() -> ErreurMetier.introuvable("Dossier"));
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        return new StatsDto(
                dossiers.countByStatut(StatutDossier.PUBLIE) + dossiers.countByStatut(StatutDossier.EN_VERIFICATION),
                dossiers.countByStatut(StatutDossier.RESOLU));
    }

    // --------------------------------------------------------------- declarant

    @Transactional
    public DossierPrive signaler(DossierForm f, Utilisateur declarant) {
        Dossier d = new Dossier(f.getType(), declarant, nouvelleReference(),
                Textes.slug(f.getTitre()) + "-" + code(5).toLowerCase());
        d.setTitre(f.getTitre().trim());
        d.setDescription(Textes.vide(f.getDescription()));
        d.setDateFaits(f.getDateFaits());
        d.setLieuPrecis(Textes.vide(f.getLieuPrecis()));
        d.setSignesDistinctifs(Textes.vide(f.getSignesDistinctifs()));
        d.setQuartier(quartiers.findById(f.getQuartierId())
                .orElseThrow(() -> ErreurMetier.invalide("QUARTIER_INCONNU", "Quartier inconnu")));

        if (f.getType() == TypeDossier.PERSONNE) {
            if (Textes.vide(f.getNomComplet()) == null) {
                throw ErreurMetier.invalide("NOM_OBLIGATOIRE", "Le nom de la personne est obligatoire");
            }
            d.setNomComplet(f.getNomComplet().trim());
            d.setSexe(Textes.vide(f.getSexe()));
            d.setAge(f.getAge());
            d.setDescriptionPhysique(Textes.vide(f.getDescriptionPhysique()));
            d.setCirconstances(Textes.vide(f.getCirconstances()));
        } else {
            if (f.getCategorieId() == null) {
                throw ErreurMetier.invalide("CATEGORIE_OBLIGATOIRE", "La catégorie de l'objet est obligatoire");
            }
            d.setCategorie(categories.findById(f.getCategorieId())
                    .orElseThrow(() -> ErreurMetier.invalide("CATEGORIE_INCONNUE", "Catégorie inconnue")));
            d.setMarque(Textes.vide(f.getMarque()));
            d.setModele(Textes.vide(f.getModele()));
            d.setCouleur(Textes.vide(f.getCouleur()));
        }

        if (f.getPhoto() != null && !f.getPhoto().isEmpty()) {
            d.setPhoto(stockage.enregistrerImage(f.getPhoto()));
        }
        return DossierPrive.de(dossiers.save(d));
    }

    @Transactional(readOnly = true)
    public List<DossierPrive> mesDossiers(Utilisateur declarant) {
        return dossiers.findByDeclarantOrderByDateSoumissionDesc(declarant).stream().map(DossierPrive::de).toList();
    }

    /** RG10 : une retrouvaille declaree met le dossier en verification, jamais en resolu. */
    @Transactional
    public DossierPrive declarerRetrouve(Long id, String motif, Utilisateur declarant) {
        Dossier d = charger(id);
        if (!d.appartientA(declarant)) throw ErreurMetier.introuvable("Dossier");
        if (d.getStatut() != StatutDossier.PUBLIE) {
            throw ErreurMetier.conflit("TRANSITION_INTERDITE", "Seul un avis en ligne peut être déclaré retrouvé");
        }
        d.transitionner(StatutDossier.EN_VERIFICATION, declarant, ErreurMetier.exigerMotif(motif));
        return DossierPrive.de(d);
    }

    // -------------------------------------------------------------- moderation

    @Transactional(readOnly = true)
    public List<DossierPrive> aModerer() {
        return dossiers.findByStatutInOrderByDateSoumissionAsc(
                List.of(StatutDossier.EN_ATTENTE, StatutDossier.EN_VERIFICATION)).stream().map(DossierPrive::de).toList();
    }

    @Transactional
    public DossierPrive decider(Long id, DecisionModeration decision, String motif, Utilisateur moderateur) {
        Dossier d = charger(id);
        if (!decision.depuis.contains(d.getStatut())) {
            throw ErreurMetier.conflit("TRANSITION_INTERDITE",
                    "Action impossible sur un dossier " + d.getStatut().libelle());
        }
        String motifRetenu = decision.motifObligatoire ? ErreurMetier.exigerMotif(motif)
                : (motif == null || motif.isBlank() ? "Validé" : motif.trim());

        StatutDossier ancien = d.getStatut();
        boolean premierePublication = d.transitionner(decision.cible, moderateur, motifRetenu);

        evenements.publishEvent(new StatutDossierChange(d.getId(), ancien, decision.cible, motifRetenu));
        if (premierePublication) {
            evenements.publishEvent(new DossierPublie(d.getId()));
        }
        return DossierPrive.de(d);
    }

    /** CA14 : suspension de visibilite sans toucher au statut. */
    @Transactional
    public DossierPrive masquer(Long id, boolean masquer, String motif, Utilisateur moderateur) {
        Dossier d = charger(id);
        d.masquer(masquer, moderateur, ErreurMetier.exigerMotif(motif));
        return DossierPrive.de(d);
    }

    // ----------------------------------------------------------------- interne

    private Dossier charger(Long id) {
        return dossiers.findById(id).orElseThrow(() -> ErreurMetier.introuvable("Dossier"));
    }

    private String nouvelleReference() {
        String ref;
        do {
            ref = "AD-" + Year.now().getValue() + "-" + code(5);
        } while (dossiers.existsByReference(ref));
        return ref;
    }

    private String code(int longueur) {
        var sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) sb.append(ALPHABET.charAt(aleatoire.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
