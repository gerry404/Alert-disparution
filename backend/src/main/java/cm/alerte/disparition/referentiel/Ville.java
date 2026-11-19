package cm.alerte.disparition.referentiel;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "villes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ville {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nom;

    @ManyToOne(optional = false)
    @JoinColumn(name = "region_id")
    private Region region;
}
