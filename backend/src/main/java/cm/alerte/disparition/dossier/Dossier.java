package cm.alerte.disparition.dossier;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.referentiel.Categorie;
import cm.alerte.disparition.referentiel.Quartier;
import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregat principal. Personnes et objets partagent la meme table (colonne "type") :
 * c'est le mapping "une table par hierarchie" de DossierPersonne / DossierObjet.
 *
 * Le statut ne se modifie que via {@link #transitionner}, qui controle la transition
 * et ecrit l'historique dans la meme operation.
 */
@Entity
@Table(name = "dossiers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TypeDossier type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatutDossier statut;

    /** Suspension de visibilite, independante du statut (section 6.1). */
    @Column(nullable = false)
    private boolean masque;

    @ManyToOne(optional = false)
    @JoinColumn(name = "declarant_id")
    private Utilisateur declarant;

    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "quartier_id")
    private Quartier quartier;

    @Setter
    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @Setter
    @Column(nullable = false, length = 120)
    private String titre;

    @Setter
    @Column(length = 2000)
    private String description;

    @Setter
    @Column(name = "date_faits", nullable = false)
    private LocalDateTime dateFaits;

    /** Adresse precise : jamais exposee publiquement (RG14). */
    @Setter
    @Column(name = "lieu_precis", length = 255)
    private String lieuPrecis;

    @Setter
    @Column(length = 255)
    private String photo;

    @Setter
    @Column(name = "nom_complet", length = 150)
    private String nomComplet;

    @Setter
    @Column(length = 1)
    private String sexe;

    @Setter
    private Integer age;

    @Setter
    @Column(name = "description_physique", length = 1000)
    private String descriptionPhysique;

    @Setter
    @Column(length = 2000)
    private String circonstances;

    @Setter
    @Column(length = 80)
    private String marque;

    @Setter
    @Column(length = 80)
    private String modele;

    @Setter
    @Column(length = 40)
    private String couleur;

    @Setter
    @Column(name = "signes_distinctifs", length = 1000)
    private String signesDistinctifs;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.PERSIST)
    @OrderBy("date ASC, id ASC")
    private List<HistoriqueStatut> historique = new ArrayList<>();

    Dossier(TypeDossier type, Utilisateur declarant, String reference, String slug) {
        this.type = type;
        this.declarant = declarant;
        this.reference = reference;
        this.slug = slug;
        this.statut = StatutDossier.EN_ATTENTE;
        this.dateSoumission = LocalDateTime.now();
        this.historique.add(new HistoriqueStatut(this, null, StatutDossier.EN_ATTENTE, declarant, "Signalement soumis"));
    }

    /**
     * Seul point d'entree pour changer le statut.
     * @return true si c'est la toute premiere publication (declenche les alertes par zone)
     */
    boolean transitionner(StatutDossier cible, Utilisateur auteur, String motif) {
        if (!statut.peutPasserA(cible)) {
            throw ErreurMetier.conflit("TRANSITION_INTERDITE",
                    "Impossible de passer un dossier " + statut.libelle() + " à " + cible.libelle());
        }
        historique.add(new HistoriqueStatut(this, statut, cible, auteur, motif));
        statut = cible;
        boolean premierePublication = cible == StatutDossier.PUBLIE && datePublication == null;
        if (premierePublication) {
            datePublication = LocalDateTime.now();
        }
        return premierePublication;
    }

    void masquer(boolean valeur, Utilisateur auteur, String motif) {
        if (masque == valeur) return;
        masque = valeur;
        historique.add(new HistoriqueStatut(this, statut, statut, auteur,
                (valeur ? "Visibilité suspendue : " : "Visibilité rétablie : ") + motif));
    }

    public List<HistoriqueStatut> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    public boolean estMineur() {
        return type == TypeDossier.PERSONNE && age != null && age < 18;
    }

    /** RG01, RG05, CA04, CA14 : ce qu'un visiteur a le droit de voir. */
    public boolean estVisiblePubliquement() {
        return statut.estPublic() && !masque && !estMineur();
    }

    public boolean appartientA(Utilisateur u) {
        return declarant.getId().equals(u.getId());
    }

    public boolean photoPubliable() {
        return photo != null && (categorie == null || !categorie.isSensible());
    }
}
