package cm.alerte.disparition.model;

import cm.alerte.disparition.model.Enums.StatutDossier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class HistoriqueStatut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Dossier dossier;

    @ManyToOne
    private Utilisateur auteur;

    @Enumerated(EnumType.STRING)
    private StatutDossier ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDossier nouveauStatut;

    @Column(length = 1000)
    private String motif;

    private LocalDateTime date = LocalDateTime.now();
}
