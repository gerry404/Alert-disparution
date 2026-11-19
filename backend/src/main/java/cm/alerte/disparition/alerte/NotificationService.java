package cm.alerte.disparition.alerte;

import cm.alerte.disparition.commun.ErreurMetier;
import cm.alerte.disparition.commun.PageDto;
import cm.alerte.disparition.utilisateur.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public record NotificationDto(Long id, Notification.Type type, String titre, String message, String lien,
                                  boolean lue, LocalDateTime creeLe) {
        static NotificationDto de(Notification n) {
            return new NotificationDto(n.getId(), n.getType(), n.getTitre(), n.getMessage(), n.getLien(),
                    n.isLue(), n.getCreeLe());
        }
    }

    private final NotificationRepository notifications;
    private final List<CanalNotification> canaux;

    public NotificationService(NotificationRepository notifications, List<CanalNotification> canaux) {
        this.notifications = notifications;
        this.canaux = canaux;
    }

    /** Enregistre puis relaie sur chaque canal. Un canal en panne n'empeche pas les autres. */
    @Transactional
    public void notifier(Utilisateur destinataire, Notification.Type type, String titre, String message, String lien) {
        Notification n = notifications.save(new Notification(destinataire, type, titre, message, lien));
        for (CanalNotification canal : canaux) {
            try {
                canal.envoyer(n);
            } catch (RuntimeException e) {
                log.warn("Canal {} en échec pour la notification {}", canal.nom(), n.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageDto<NotificationDto> lister(Utilisateur u, int page) {
        return PageDto.de(notifications.findByDestinataireOrderByCreeLeDesc(u, PageRequest.of(Math.max(page, 0), 20)),
                NotificationDto::de);
    }

    @Transactional(readOnly = true)
    public long nonLues(Utilisateur u) {
        return notifications.countByDestinataireAndLueFalse(u);
    }

    @Transactional
    public void marquerLue(Long id, Utilisateur u) {
        Notification n = notifications.findById(id)
                .filter(x -> x.getDestinataire().getId().equals(u.getId()))
                .orElseThrow(() -> ErreurMetier.introuvable("Notification"));
        n.marquerLue();
    }

    @Transactional
    public void toutMarquerLu(Utilisateur u) {
        notifications.toutMarquerLu(u);
    }
}
