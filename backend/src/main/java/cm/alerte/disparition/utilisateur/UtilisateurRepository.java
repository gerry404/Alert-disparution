package cm.alerte.disparition.utilisateur;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByTelephone(String telephone);

    boolean existsByTelephone(String telephone);

    @Query("""
            select u from Utilisateur u
            where :q is null
               or lower(u.nom) like :q or lower(u.prenom) like :q or u.telephone like :q
            """)
    Page<Utilisateur> rechercher(@Param("q") String q, Pageable pageable);
}
