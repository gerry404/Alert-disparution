package cm.alerte.disparition.temoignage;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.dossier.Dossier;
import cm.alerte.disparition.referentiel.Quartier;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Temoignage SANS compte (decision du groupe, ecart assume avec la section 5.6.1 du CDC) :
 * on garde un numero pour pouvoir rappeler, jamais affiche publiquement (RG04, RG06).
 */
@Entity
@Table(name = "temoignages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Temoignage {

    public enum Type { APERCU, INFORMATION }

    public enum Statut {
        EN_ATTENTE, PUBLIE, REJETE, MASQUE;

        Set<Statut> suivants() {
            return switch (this) {
                case EN_ATTENTE -> EnumSet.of(PUBLIE, REJETE);
                case PUBLIE -> EnumSet.of(MASQUE);
                case REJETE, MASQUE -> EnumSet.noneOf(Statut.class);
            };
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(name = "date_faits", nullable = false)
    private LocalDateTime dateFaits;

    @ManyToOne
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    @Column(name = "lieu_precis", length = 255)
    private String lieuPrecis;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "nom_temoin", length = 100)
    private String nomTemoin;

    @Column(name = "telephone_temoin", nullable = false, length = 20)
    private String telephoneTemoin;

    @Column(nullable = false)
    private boolean anonyme;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.EN_ATTENTE;

    @ManyToOne
    @JoinColumn(name = "traite_par_id")
    private Utilisateur traitePar;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    Temoignage(Dossier dossier, Type type, LocalDateTime dateFaits, Quartier quartier, String lieuPrecis,
               String description, String nomTemoin, String telephoneTemoin, boolean anonyme) {
        this.dossier = dossier;
        this.type = type;
        this.dateFaits = dateFaits;
        this.quartier = quartier;
        this.lieuPrecis = lieuPrecis;
        this.description = description;
        this.nomTemoin = nomTemoin;
        this.telephoneTemoin = telephoneTemoin;
        this.anonyme = anonyme;
    }

    void passerA(Statut cible, Utilisateur moderateur) {
        if (!statut.suivants().contains(cible)) {
            throw ErreurMetier.conflit("TRANSITION_INTERDITE", "Ce témoignage a déjà été traité");
        }
        statut = cible;
        traitePar = moderateur;
    }

    /** Nom affichable publiquement : jamais si anonyme ou non renseigne. */
    String auteurPublic() {
        return anonyme || nomTemoin == null ? "Témoin anonyme" : nomTemoin;
    }
}
