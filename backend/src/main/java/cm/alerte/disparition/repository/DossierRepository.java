package cm.alerte.disparition.repository;

import cm.alerte.disparition.model.Dossier;
import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Enums.TypeDossier;
import cm.alerte.disparition.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier, Long> {

    Optional<Dossier> findBySlug(String slug);

    List<Dossier> findByDeclarantOrderByDateSoumissionDesc(Utilisateur declarant);

    List<Dossier> findByStatutInOrderByDateSoumissionAsc(Collection<StatutDossier> statuts);

    long countByStatut(StatutDossier statut);

    /**
     * Recherche publique : on ne renvoie que ce qui a ete valide, non masque,
     * et jamais un mineur (RG05).
     */
    @Query("""
            select d from Dossier d
            where d.statut in :statuts
              and d.masque = false
              and (d.type <> cm.alerte.disparition.model.Enums.TypeDossier.PERSONNE
                   or d.age is null or d.age >= 18)
              and (:type is null or d.type = :type)
              and (:quartierId is null or d.quartier.id = :quartierId)
              and (:q is null
                   or lower(d.titre) like :q
                   or lower(d.description) like :q
                   or lower(d.signesDistinctifs) like :q
                   or lower(d.reference) like :q)
            order by d.datePublication desc
            """)
    List<Dossier> rechercher(@Param("statuts") Collection<StatutDossier> statuts,
                             @Param("type") TypeDossier type,
                             @Param("quartierId") Long quartierId,
                             @Param("q") String q);
}
