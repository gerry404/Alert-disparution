package cm.alerte.disparition.controller;

import cm.alerte.disparition.dto.Dto.CategorieDto;
import cm.alerte.disparition.dto.Dto.QuartierDto;
import cm.alerte.disparition.dto.Dto.StatsDto;
import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Enums.StatutTemoignage;
import cm.alerte.disparition.repository.CategorieRepository;
import cm.alerte.disparition.repository.DossierRepository;
import cm.alerte.disparition.repository.QuartierRepository;
import cm.alerte.disparition.repository.TemoignageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiel")
public class ReferentielController {

    private final QuartierRepository quartiers;
    private final CategorieRepository categories;
    private final DossierRepository dossiers;
    private final TemoignageRepository temoignages;

    public ReferentielController(QuartierRepository quartiers, CategorieRepository categories,
                                 DossierRepository dossiers, TemoignageRepository temoignages) {
        this.quartiers = quartiers;
        this.categories = categories;
        this.dossiers = dossiers;
        this.temoignages = temoignages;
    }

    @GetMapping("/quartiers")
    public List<QuartierDto> quartiers() {
        return quartiers.findAllByOrderByNomAsc().stream().map(QuartierDto::de).toList();
    }

    @GetMapping("/categories")
    public List<CategorieDto> categories() {
        return categories.findAllByOrderByLibelleAsc().stream().map(CategorieDto::de).toList();
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        return new StatsDto(
                dossiers.countByStatut(StatutDossier.PUBLIE) + dossiers.countByStatut(StatutDossier.EN_VERIFICATION),
                dossiers.countByStatut(StatutDossier.RESOLU),
                temoignages.countByStatut(StatutTemoignage.PUBLIE));
    }
}
