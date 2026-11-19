package cm.alerte.disparition.repository;

import cm.alerte.disparition.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface TemoignageRepository extends JpaRepository<Temoignage, Long> {

    List<Temoignage> findByDossierAndStatutOrderByDateFaitsDesc(Dossier dossier, Enums.StatutTemoignage statut);

    List<Temoignage> findByStatutOrderByDateSoumissionAsc(Enums.StatutTemoignage statut);

    long countByStatut(Enums.StatutTemoignage statut);
}
