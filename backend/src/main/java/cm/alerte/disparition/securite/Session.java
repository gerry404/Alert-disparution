package cm.alerte.disparition.securite;

import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session opaque. On ne stocke que l'empreinte SHA-256 du token :
 * une fuite de la base ne permet pas de se connecter a la place de quelqu'un.
 */
@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "empreinte_token", nullable = false, unique = true, length = 64)
    private String empreinteToken;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    Session(Utilisateur utilisateur, String empreinteToken, LocalDateTime creeLe, LocalDateTime expireLe) {
        this.utilisateur = utilisateur;
        this.empreinteToken = empreinteToken;
        this.creeLe = creeLe;
        this.expireLe = expireLe;
    }

    boolean estValide(LocalDateTime maintenant) {
        return expireLe.isAfter(maintenant) && utilisateur.isActif();
    }
}
