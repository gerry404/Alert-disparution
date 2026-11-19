package cm.alerte.disparition.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    // CNI, passeport, carte bancaire... : la photo n'est jamais publiee
    private boolean sensible;

    public Categorie(String libelle, boolean sensible) {
        this.libelle = libelle;
        this.sensible = sensible;
    }
}
