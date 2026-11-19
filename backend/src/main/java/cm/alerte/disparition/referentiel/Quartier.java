package cm.alerte.disparition.referentiel;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quartiers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quartier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(length = 40)
    private String arrondissement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ville_id")
    private Ville ville;
}
