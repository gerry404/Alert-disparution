package cm.alerte.disparition.repository;

import cm.alerte.disparition.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByTelephone(String telephone);

    Optional<Utilisateur> findByToken(String token);

    boolean existsByTelephone(String telephone);
}
