package cm.alerte.disparition.referentiel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuartierRepository extends JpaRepository<Quartier, Long> {

    List<Quartier> findAllByOrderByNomAsc();
}
