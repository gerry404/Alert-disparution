package cm.alerte.disparition.rapprochement;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.dossier.Dossier;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "correspondances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"objet_perdu_id", "objet_trouve_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Correspondance {

    public enum Statut { A_EXAMINER, VALIDEE, ECARTEE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "objet_perdu_id")
    private Dossier objetPerdu;

    @ManyToOne(optional = false)
    @JoinColumn(name = "objet_trouve_id")
    private Dossier objetTrouve;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, length = 500)
    private String raisons;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.A_EXAMINER;

    @ManyToOne
    @JoinColumn(name = "traitee_par_id")
    private Utilisateur traiteePar;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    @Column(name = "traitee_le")
    private LocalDateTime traiteeLe;

    Correspondance(Dossier objetPerdu, Dossier objetTrouve, int score, String raisons) {
        this.objetPerdu = objetPerdu;
        this.objetTrouve = objetTrouve;
        this.score = score;
        this.raisons = raisons.length() > 500 ? raisons.substring(0, 500) : raisons;
    }

    void trancher(Statut decision, Utilisateur moderateur) {
        if (statut != Statut.A_EXAMINER) {
            throw ErreurMetier.conflit("DEJA_TRAITEE", "Cette correspondance a déjà été examinée");
        }
        statut = decision;
        traiteePar = moderateur;
        traiteeLe = LocalDateTime.now();
    }
}
