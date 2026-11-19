package cm.alerte.disparition.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Journal : qui a regarde l'identite d'un temoin, quand et pourquoi.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AccesIdentite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Temoignage temoignage;

    @ManyToOne(optional = false)
    private Utilisateur moderateur;

    @Column(nullable = false)
    private String motif;

    private LocalDateTime date = LocalDateTime.now();
}
