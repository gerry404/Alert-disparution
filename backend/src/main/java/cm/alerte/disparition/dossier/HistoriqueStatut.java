package cm.alerte.disparition.dossier;

import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Une ligne par decision. Jamais modifiee ni supprimee (RG12, RG13). */
@Entity
@Table(name = "historique_statuts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoriqueStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "auteur_id")
    private Utilisateur auteur;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ancien_statut", length = 20)
    private StatutDossier ancienStatut;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "nouveau_statut", nullable = false, length = 20)
    private StatutDossier nouveauStatut;

    @Column(length = 1000)
    private String motif;

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    HistoriqueStatut(Dossier dossier, StatutDossier ancien, StatutDossier nouveau, Utilisateur auteur, String motif) {
        this.dossier = dossier;
        this.ancienStatut = ancien;
        this.nouveauStatut = nouveau;
        this.auteur = auteur;
        this.motif = motif;
    }
}
