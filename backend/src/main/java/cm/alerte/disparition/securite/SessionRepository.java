package cm.alerte.disparition.securite;

import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByEmpreinteToken(String empreinte);

    @Modifying
    @Query("delete from Session s where s.empreinteToken = :empreinte")
    void supprimerParEmpreinte(String empreinte);

    @Modifying
    @Query("delete from Session s where s.utilisateur = :utilisateur")
    void supprimerToutes(Utilisateur utilisateur);

    @Modifying
    @Query("delete from Session s where s.expireLe < :date")
    int supprimerExpirees(LocalDateTime date);
}
