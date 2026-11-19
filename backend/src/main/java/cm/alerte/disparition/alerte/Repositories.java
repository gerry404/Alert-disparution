package cm.alerte.disparition.alerte;

import cm.alerte.disparition.utilisateur.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface AbonnementRepository extends JpaRepository<AbonnementAlerte, Long> {

    List<AbonnementAlerte> findByUtilisateurOrderByCreeLeAsc(Utilisateur utilisateur);

    Optional<AbonnementAlerte> findByUtilisateurAndQuartierId(Utilisateur utilisateur, Long quartierId);

    @Query("select a from AbonnementAlerte a where a.quartier.id = :quartierId and a.actif = true and a.utilisateur.actif = true")
    List<AbonnementAlerte> actifsPourQuartier(Long quartierId);
}

interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByDestinataireOrderByCreeLeDesc(Utilisateur destinataire, Pageable pageable);

    long countByDestinataireAndLueFalse(Utilisateur destinataire);

    @Modifying
    @Query("update Notification n set n.lue = true where n.destinataire = :destinataire and n.lue = false")
    int toutMarquerLu(Utilisateur destinataire);
}
