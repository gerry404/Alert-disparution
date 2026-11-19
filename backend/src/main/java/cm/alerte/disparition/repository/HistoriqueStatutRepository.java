package cm.alerte.disparition.repository;

import cm.alerte.disparition.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface HistoriqueStatutRepository extends JpaRepository<HistoriqueStatut, Long> {

    List<HistoriqueStatut> findByDossierOrderByDateAsc(Dossier dossier);
}
