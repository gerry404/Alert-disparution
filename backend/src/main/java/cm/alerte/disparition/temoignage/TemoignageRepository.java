package cm.alerte.disparition.temoignage;

import cm.alerte.disparition.dossier.Dossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TemoignageRepository extends JpaRepository<Temoignage, Long> {

    List<Temoignage> findByDossierAndStatutOrderByDateFaitsDesc(Dossier dossier, Temoignage.Statut statut);

    List<Temoignage> findByStatutOrderByDateSoumissionAsc(Temoignage.Statut statut);
}

interface AccesIdentiteRepository extends JpaRepository<AccesIdentite, Long> {

    Page<AccesIdentite> findAllByOrderByDateDesc(Pageable pageable);
}
