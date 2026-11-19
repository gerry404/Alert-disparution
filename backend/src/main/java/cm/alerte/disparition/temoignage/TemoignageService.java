package cm.alerte.disparition.temoignage;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.commun.LimiteurDebit;
import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.commun.Textes;
import cm.alerte.disparition.dossier.Dossier;
import cm.alerte.disparition.dossier.DossierRepository;
import cm.alerte.disparition.dossier.StatutDossier;
import cm.alerte.disparition.referentiel.QuartierRepository;
import cm.alerte.disparition.temoignage.TemoignageDtos.*;
import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class TemoignageService {

    private final TemoignageRepository temoignages;
    private final AccesIdentiteRepository acces;
    private final DossierRepository dossiers;
    private final QuartierRepository quartiers;
    private final LimiteurDebit limiteur;
    private final ApplicationEventPublisher evenements;
    private final int maxParHeure;

    public TemoignageService(TemoignageRepository temoignages, AccesIdentiteRepository acces,
                             DossierRepository dossiers, QuartierRepository quartiers, LimiteurDebit limiteur,
                             ApplicationEventPublisher evenements,
                             @Value("${app.limites.temoignages-par-heure:5}") int maxParHeure) {
        this.temoignages = temoignages;
        this.acces = acces;
        this.dossiers = dossiers;
        this.quartiers = quartiers;
        this.limiteur = limiteur;
        this.evenements = evenements;
        this.maxParHeure = maxParHeure;
    }

    /** Ouvert a tous, donc protege contre le spam par IP et par numero. */
    @Transactional
    public void soumettre(Long dossierId, TemoignageRequest req, String ip) {
        String tel = Textes.telephone(req.telephoneTemoin());
        limiteur.verifier("temoignage-ip:" + ip, maxParHeure, Duration.ofHours(1));
        limiteur.verifier("temoignage-tel:" + tel, maxParHeure, Duration.ofHours(1));

        Dossier d = dossiers.findById(dossierId)
                .filter(Dossier::estVisiblePubliquement)
                .filter(x -> x.getStatut() != StatutDossier.RESOLU)
                .orElseThrow(() -> ErreurMetier.introuvable("Dossier"));

        var quartier = req.quartierId() == null ? null : quartiers.findById(req.quartierId()).orElse(null);
        temoignages.save(new Temoignage(d, req.type(), req.dateFaits(), quartier, Textes.vide(req.lieuPrecis()),
                req.description().trim(), Textes.vide(req.nomTemoin()), tel, req.anonyme()));
    }

    @Transactional(readOnly = true)
    public List<TemoignagePublic> publies(Long dossierId) {
        Dossier d = dossiers.findById(dossierId)
                .filter(Dossier::estVisiblePubliquement)
                .orElseThrow(() -> ErreurMetier.introuvable("Dossier"));
        return temoignages.findByDossierAndStatutOrderByDateFaitsDesc(d, Temoignage.Statut.PUBLIE)
                .stream().map(TemoignagePublic::de).toList();
    }

    @Transactional(readOnly = true)
    public List<TemoignageModeration> enAttente() {
        return temoignages.findByStatutOrderByDateSoumissionAsc(Temoignage.Statut.EN_ATTENTE)
                .stream().map(TemoignageModeration::de).toList();
    }

    @Transactional
    public void traiter(Long id, Temoignage.Statut cible, Utilisateur moderateur) {
        Temoignage t = charger(id);
        t.passerA(cible, moderateur);
        if (cible == Temoignage.Statut.PUBLIE) {
            evenements.publishEvent(new TemoignagePublie(t.getDossier().getId(), t.getId()));
        }
    }

    /** L'identite ne sort qu'avec un motif, et chaque consultation est journalisee. */
    @Transactional
    public IdentiteTemoin consulterIdentite(Long id, String motif, Utilisateur moderateur) {
        Temoignage t = charger(id);
        acces.save(new AccesIdentite(t, moderateur, ErreurMetier.exigerMotif(motif)));
        return new IdentiteTemoin(t.getNomTemoin(), t.getTelephoneTemoin());
    }

    @Transactional(readOnly = true)
    public PageDto<AccesIdentiteDto> journal(int page) {
        return PageDto.de(acces.findAllByOrderByDateDesc(PageRequest.of(Math.max(page, 0), 30)), AccesIdentiteDto::de);
    }

    private Temoignage charger(Long id) {
        return temoignages.findById(id).orElseThrow(() -> ErreurMetier.introuvable("Témoignage"));
    }
}
