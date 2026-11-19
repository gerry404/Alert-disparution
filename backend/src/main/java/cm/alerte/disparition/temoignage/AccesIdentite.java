package cm.alerte.disparition.temoignage;

import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Journal : qui a consulte l'identite d'un temoin, quand et pourquoi (5.6.1, regle 5). */
@Entity
@Table(name = "acces_identites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccesIdentite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "temoignage_id")
    private Temoignage temoignage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moderateur_id")
    private Utilisateur moderateur;

    @Column(nullable = false, length = 255)
    private String motif;

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    AccesIdentite(Temoignage temoignage, Utilisateur moderateur, String motif) {
        this.temoignage = temoignage;
        this.moderateur = moderateur;
        this.motif = motif;
    }
}
