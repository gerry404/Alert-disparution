package cm.alerte.disparition.alerte;

import cm.alerte.disparition.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Notification "dans l'application". C'est la trace de reference :
 * les autres canaux (SMS, WhatsApp, e-mail...) ne font que la relayer.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    public enum Type {
        NOUVELLE_DISPARITION,
        TEMOIGNAGE_PUBLIE,
        STATUT_DOSSIER,
        CORRESPONDANCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 255)
    private String lien;

    @Column(nullable = false)
    private boolean lue;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    Notification(Utilisateur destinataire, Type type, String titre, String message, String lien) {
        this.destinataire = destinataire;
        this.type = type;
        this.titre = titre;
        this.message = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        this.lien = lien;
    }

    void marquerLue() {
        this.lue = true;
    }
}
