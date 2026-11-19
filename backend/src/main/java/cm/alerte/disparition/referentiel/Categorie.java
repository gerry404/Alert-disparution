package cm.alerte.disparition.referentiel;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String libelle;

    /** Pieces d'identite, cartes bancaires... : jamais de photo publique. */
    @Column(nullable = false)
    private boolean sensible;
}
