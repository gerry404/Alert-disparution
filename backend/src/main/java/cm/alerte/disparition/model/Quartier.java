package cm.alerte.disparition.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Quartier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String arrondissement;

    @ManyToOne(optional = false)
    private Ville ville;

    public Quartier(String nom, String arrondissement, Ville ville) {
        this.nom = nom;
        this.arrondissement = arrondissement;
        this.ville = ville;
    }
}
