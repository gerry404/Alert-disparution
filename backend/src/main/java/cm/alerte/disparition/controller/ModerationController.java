package cm.alerte.disparition.controller;

import cm.alerte.disparition.dto.Dto.*;
import cm.alerte.disparition.model.AccesIdentite;
import cm.alerte.disparition.model.Dossier;
import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Enums.StatutTemoignage;
import cm.alerte.disparition.model.Temoignage;
import cm.alerte.disparition.model.Utilisateur;
import cm.alerte.disparition.repository.AccesIdentiteRepository;
import cm.alerte.disparition.repository.DossierRepository;
import cm.alerte.disparition.repository.TemoignageRepository;
import cm.alerte.disparition.service.DossierService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final DossierRepository dossiers;
    private final TemoignageRepository temoignages;
    private final AccesIdentiteRepository acces;
    private final DossierService service;

    public ModerationController(DossierRepository dossiers, TemoignageRepository temoignages,
                                AccesIdentiteRepository acces, DossierService service) {
        this.dossiers = dossiers;
        this.temoignages = temoignages;
        this.acces = acces;
        this.service = service;
    }

    // ---------- Dossiers ----------

    @GetMapping("/dossiers")
    public List<DossierPrive> aTraiter() {
        return dossiers.findByStatutInOrderByDateSoumissionAsc(
                        List.of(StatutDossier.EN_ATTENTE, StatutDossier.EN_VERIFICATION))
                .stream().map(service::versPrive).toList();
    }

    @PostMapping("/dossiers/{id}/publier")
    public DossierPrive publier(@PathVariable Long id, @RequestBody(required = false) MotifRequest req,
                                @AuthenticationPrincipal Utilisateur m) {
        return transition(id, Set.of(StatutDossier.EN_ATTENTE), StatutDossier.PUBLIE, m, motif(req, "Validé"));
    }

    @PostMapping("/dossiers/{id}/rejeter")
    public DossierPrive rejeter(@PathVariable Long id, @RequestBody MotifRequest req,
                                @AuthenticationPrincipal Utilisateur m) {
        exigerMotif(req);
        return transition(id, Set.of(StatutDossier.EN_ATTENTE), StatutDossier.REJETE, m, req.motif());
    }

    @PostMapping("/dossiers/{id}/resoudre")
    public DossierPrive resoudre(@PathVariable Long id, @RequestBody MotifRequest req,
                                 @AuthenticationPrincipal Utilisateur m) {
        exigerMotif(req); // RG11 : la resolution doit etre justifiee
        return transition(id, Set.of(StatutDossier.EN_VERIFICATION, StatutDossier.PUBLIE),
                StatutDossier.RESOLU, m, req.motif());
    }

    /** Retrouvaille non confirmee : le dossier redevient une recherche active. */
    @PostMapping("/dossiers/{id}/rouvrir")
    public DossierPrive rouvrir(@PathVariable Long id, @RequestBody MotifRequest req,
                                @AuthenticationPrincipal Utilisateur m) {
        exigerMotif(req);
        return transition(id, Set.of(StatutDossier.EN_VERIFICATION, StatutDossier.RESOLU),
                StatutDossier.PUBLIE, m, req.motif());
    }

    private DossierPrive transition(Long id, Set<StatutDossier> depuis, StatutDossier vers,
                                    Utilisateur m, String motif) {
        Dossier d = dossiers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!depuis.contains(d.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Action impossible depuis le statut " + d.getStatut());
        }
        service.changerStatut(d, vers, m, motif);
        return service.versPrive(d);
    }

    // ---------- Temoignages ----------

    @GetMapping("/temoignages")
    public List<TemoignageModeration> temoignagesEnAttente() {
        return temoignages.findByStatutOrderByDateSoumissionAsc(StatutTemoignage.EN_ATTENTE).stream()
                .map(t -> new TemoignageModeration(TemoignagePublic.de(t), t.isAnonyme(), t.getLieuPrecis(),
                        t.getDossier().getTitre(), t.getDossier().getReference()))
                .toList();
    }

    @PostMapping("/temoignages/{id}/publier")
    public void publierTemoignage(@PathVariable Long id, @AuthenticationPrincipal Utilisateur m) {
        changerTemoignage(id, StatutTemoignage.PUBLIE, m);
    }

    @PostMapping("/temoignages/{id}/rejeter")
    public void rejeterTemoignage(@PathVariable Long id, @AuthenticationPrincipal Utilisateur m) {
        changerTemoignage(id, StatutTemoignage.REJETE, m);
    }

    /**
     * Voir qui a temoigne. Volontairement separe de la liste : il faut donner
     * un motif, et chaque consultation est enregistree.
     */
    @PostMapping("/temoignages/{id}/identite")
    public IdentiteTemoin voirIdentite(@PathVariable Long id, @RequestBody MotifRequest req,
                                       @AuthenticationPrincipal Utilisateur m) {
        exigerMotif(req);
        Temoignage t = temoignages.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AccesIdentite a = new AccesIdentite();
        a.setTemoignage(t);
        a.setModerateur(m);
        a.setMotif(req.motif());
        acces.save(a);
        return new IdentiteTemoin(t.getNomTemoin(), t.getTelephoneTemoin());
    }

    private void changerTemoignage(Long id, StatutTemoignage statut, Utilisateur m) {
        Temoignage t = temoignages.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        t.setStatut(statut);
        t.setValidePar(m);
        temoignages.save(t);
    }

    private static void exigerMotif(MotifRequest req) {
        if (req == null || req.motif() == null || req.motif().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un motif est obligatoire");
        }
    }

    private static String motif(MotifRequest req, String parDefaut) {
        return req == null || req.motif() == null || req.motif().isBlank() ? parDefaut : req.motif();
    }
}
