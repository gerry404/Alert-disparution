package cm.alerte.disparition.model;

import cm.alerte.disparition.model.Enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.DECLARANT;

    private boolean actif = true;

    // token de session tres simple (pas de JWT pour le MVP)
    @Column(unique = true)
    private String token;

    private LocalDateTime dateInscription = LocalDateTime.now();

    public String getNomAffiche() {
        return prenom == null ? nom : prenom + " " + nom;
    }

    public boolean estModerateur() {
        return role == Role.MODERATEUR || role == Role.ADMIN;
    }
}
