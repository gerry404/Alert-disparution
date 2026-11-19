package cm.alerte.disparition.dossier;

import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier, Long>, JpaSpecificationExecutor<Dossier> {

    Optional<Dossier> findBySlug(String slug);

    boolean existsByReference(String reference);

    List<Dossier> findByDeclarantOrderByDateSoumissionDesc(Utilisateur declarant);

    List<Dossier> findByStatutInOrderByDateSoumissionAsc(Collection<StatutDossier> statuts);

    long countByStatut(StatutDossier statut);

    /** Objets de type oppose, publies, dans la meme ville, sur une fenetre de dates. */
    @Query("""
            select d from Dossier d
            where d.type = :type
              and d.statut = cm.alerte.disparition.dossier.StatutDossier.PUBLIE
              and d.masque = false
              and d.quartier.ville.id = :villeId
              and d.dateFaits between :debut and :fin
            """)
    List<Dossier> candidatsRapprochement(TypeDossier type, Long villeId, LocalDateTime debut, LocalDateTime fin);

    /** Filtres de la recherche publique, combinables. */
    final class Filtres {

        private Filtres() {
        }

        static Specification<Dossier> visibles(boolean inclureResolus) {
            return (root, query, cb) -> {
                var statuts = inclureResolus
                        ? List.of(StatutDossier.PUBLIE, StatutDossier.EN_VERIFICATION, StatutDossier.RESOLU)
                        : List.of(StatutDossier.PUBLIE, StatutDossier.EN_VERIFICATION);
                var age = root.<Integer>get("age");
                return cb.and(
                        root.get("statut").in(statuts),
                        cb.isFalse(root.get("masque")),
                        // RG05 : aucun mineur dans les resultats publics
                        cb.or(cb.notEqual(root.get("type"), TypeDossier.PERSONNE), cb.isNull(age), cb.ge(age, 18)));
            };
        }

        static Specification<Dossier> deType(TypeDossier type) {
            return (root, query, cb) -> cb.equal(root.get("type"), type);
        }

        static Specification<Dossier> dansQuartier(Long quartierId) {
            return (root, query, cb) -> cb.equal(root.get("quartier").get("id"), quartierId);
        }

        static Specification<Dossier> contient(String texte) {
            String motif = "%" + texte.toLowerCase() + "%";
            return (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("titre")), motif),
                    cb.like(cb.lower(root.get("description")), motif),
                    cb.like(cb.lower(root.get("signesDistinctifs")), motif),
                    cb.like(cb.lower(root.get("nomComplet")), motif),
                    cb.like(cb.lower(root.get("reference")), motif));
        }
    }
}
