package cm.alerte.disparition.model;

import cm.alerte.disparition.model.Enums.StatutDossier;
import cm.alerte.disparition.model.Enums.TypeDossier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Une seule table pour les personnes et les objets : c'est la colonne "type"
 * qui fait la difference. Les champs qui ne concernent pas le type restent vides.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeDossier type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDossier statut = StatutDossier.EN_ATTENTE;

    // suspension de visibilite sans changer le statut
    private boolean masque;

    @ManyToOne(optional = false)
    private Utilisateur declarant;

    @ManyToOne(optional = false)
    private Quartier quartier;

    @ManyToOne
    private Categorie categorie;

    @Column(nullable = false)
    private String titre;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateFaits;

    // adresse precise : visible seulement par le declarant et la moderation
    private String lieuPrecis;

    private String photo;

    // --- personne ---
    private String nomComplet;
    private String sexe;
    private Integer age;
    @Column(length = 1000)
    private String descriptionPhysique;
    @Column(length = 2000)
    private String circonstances;

    // --- objet ---
    private String marque;
    private String modele;
    private String couleur;

    @Column(length = 1000)
    private String signesDistinctifs;

    private LocalDateTime dateSoumission = LocalDateTime.now();
    private LocalDateTime datePublication;

    public boolean estMineur() {
        return type == TypeDossier.PERSONNE && age != null && age < 18;
    }

    public boolean estVisiblePubliquement() {
        return !masque && !estMineur()
                && (statut == StatutDossier.PUBLIE
                || statut == StatutDossier.EN_VERIFICATION
                || statut == StatutDossier.RESOLU);
    }
}
