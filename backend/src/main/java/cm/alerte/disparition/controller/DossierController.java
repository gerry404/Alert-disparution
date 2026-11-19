package cm.alerte.disparition.controller;

import cm.alerte.disparition.dto.Dto.*;
import cm.alerte.disparition.model.Dossier;
import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Enums.StatutTemoignage;
import cm.alerte.disparition.model.Enums.TypeDossier;
import cm.alerte.disparition.model.Temoignage;
import cm.alerte.disparition.repository.DossierRepository;
import cm.alerte.disparition.repository.QuartierRepository;
import cm.alerte.disparition.repository.TemoignageRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Partie publique : consultation, recherche, temoignage (sans compte).
 */
@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private static final List<StatutDossier> STATUTS_PUBLICS =
            List.of(StatutDossier.PUBLIE, StatutDossier.EN_VERIFICATION, StatutDossier.RESOLU);

    private final DossierRepository dossiers;
    private final TemoignageRepository temoignages;
    private final QuartierRepository quartiers;

    public DossierController(DossierRepository dossiers, TemoignageRepository temoignages,
                             QuartierRepository quartiers) {
        this.dossiers = dossiers;
        this.temoignages = temoignages;
        this.quartiers = quartiers;
    }

    @GetMapping
    public List<DossierPublic> rechercher(@RequestParam(required = false) TypeDossier type,
                                          @RequestParam(required = false) Long quartierId,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "false") boolean inclureResolus) {
        List<StatutDossier> statuts = inclureResolus ? STATUTS_PUBLICS : STATUTS_PUBLICS.subList(0, 2);
        String motif = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        return dossiers.rechercher(statuts, type, quartierId, motif).stream().map(DossierPublic::de).toList();
    }

    @GetMapping("/{slug}")
    public FicheDossier fiche(@PathVariable String slug) {
        Dossier d = dossiers.findBySlug(slug)
                .filter(Dossier::estVisiblePubliquement)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        List<TemoignagePublic> publies = temoignages
                .findByDossierAndStatutOrderByDateFaitsDesc(d, StatutTemoignage.PUBLIE)
                .stream().map(TemoignagePublic::de).toList();
        return new FicheDossier(DossierPublic.de(d), publies);
    }

    @PostMapping("/{id}/temoignages")
    @ResponseStatus(HttpStatus.CREATED)
    public void temoigner(@PathVariable Long id, @Valid @RequestBody TemoignageRequest req) {
        Dossier d = dossiers.findById(id)
                .filter(Dossier::estVisiblePubliquement)
                .filter(x -> x.getStatut() != StatutDossier.RESOLU)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable ou clos"));

        Temoignage t = new Temoignage();
        t.setDossier(d);
        t.setType(req.type());
        t.setDateFaits(req.dateFaits());
        t.setLieuPrecis(req.lieuPrecis());
        t.setDescription(req.description().trim());
        t.setNomTemoin(req.nomTemoin());
        t.setTelephoneTemoin(req.telephoneTemoin().replace(" ", ""));
        t.setAnonyme(req.anonyme());
        if (req.quartierId() != null) {
            t.setQuartier(quartiers.findById(req.quartierId()).orElse(null));
        }
        temoignages.save(t); // en attente de moderation (RG03)
    }
}
