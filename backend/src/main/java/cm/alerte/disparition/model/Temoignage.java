package cm.alerte.disparition.model;

import cm.alerte.disparition.model.Enums.StatutTemoignage;
import cm.alerte.disparition.model.Enums.TypeTemoignage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pas besoin de compte pour temoigner (choix du groupe) :
 * on garde juste un nom (facultatif) et un numero pour recontacter.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Temoignage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Dossier dossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeTemoignage type;

    @Column(nullable = false)
    private LocalDateTime dateFaits;

    @ManyToOne
    private Quartier quartier;

    private String lieuPrecis;

    @Column(nullable = false, length = 2000)
    private String description;

    private String nomTemoin;

    @Column(nullable = false, length = 20)
    private String telephoneTemoin;

    private boolean anonyme = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTemoignage statut = StatutTemoignage.EN_ATTENTE;

    @ManyToOne
    private Utilisateur validePar;

    private LocalDateTime dateSoumission = LocalDateTime.now();
}
