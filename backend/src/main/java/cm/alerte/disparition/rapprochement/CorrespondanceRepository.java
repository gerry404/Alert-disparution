package cm.alerte.disparition.rapprochement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CorrespondanceRepository extends JpaRepository<Correspondance, Long> {

    boolean existsByObjetPerduIdAndObjetTrouveId(Long perduId, Long trouveId);

    List<Correspondance> findByStatutOrderByScoreDescCreeLeAsc(Correspondance.Statut statut);
}
