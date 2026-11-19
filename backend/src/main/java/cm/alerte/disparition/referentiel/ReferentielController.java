package cm.alerte.disparition.referentiel;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** Donnees de reference : elles changent rarement, le navigateur peut les garder une heure. */
@RestController
@RequestMapping("/api/referentiel")
class ReferentielController {

    record QuartierDto(Long id, String nom, String arrondissement, String ville) {
    }

    record CategorieDto(Long id, String libelle, boolean sensible) {
    }

    private final QuartierRepository quartiers;
    private final CategorieRepository categories;

    ReferentielController(QuartierRepository quartiers, CategorieRepository categories) {
        this.quartiers = quartiers;
        this.categories = categories;
    }

    @GetMapping("/quartiers")
    @Transactional(readOnly = true)
    ResponseEntity<List<QuartierDto>> quartiers() {
        var liste = quartiers.findAllByOrderByNomAsc().stream()
                .map(q -> new QuartierDto(q.getId(), q.getNom(), q.getArrondissement(), q.getVille().getNom()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(liste);
    }

    @GetMapping("/categories")
    @Transactional(readOnly = true)
    ResponseEntity<List<CategorieDto>> categories() {
        var liste = categories.findAllByOrderByLibelleAsc().stream()
                .map(c -> new CategorieDto(c.getId(), c.getLibelle(), c.isSensible()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(liste);
    }
}
