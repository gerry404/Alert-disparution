package cm.alerte.disparition.alerte;

import cm.alerte.disparition.referentiel.Quartier;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** "Previens-moi quand une disparition est publiee dans ce quartier" (section 5.8). */
@Entity
@Table(name = "abonnements_alerte",
        uniqueConstraints = @UniqueConstraint(columnNames = {"utilisateur_id", "quartier_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbonnementAlerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    @Setter
    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    AbonnementAlerte(Utilisateur utilisateur, Quartier quartier) {
        this.utilisateur = utilisateur;
        this.quartier = quartier;
    }
}
