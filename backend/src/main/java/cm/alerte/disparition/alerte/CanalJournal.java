package cm.alerte.disparition.alerte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Canal de developpement : ecrit dans les logs ce qui partirait par SMS.
 * Tant que le cout des SMS n'est pas arbitre (12.2 du CDC), c'est lui qui tient la place.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.journal", havingValue = "true", matchIfMissing = true)
class CanalJournal implements CanalNotification {

    private static final Logger log = LoggerFactory.getLogger(CanalJournal.class);

    @Override
    public String nom() {
        return "journal";
    }

    @Override
    public void envoyer(Notification n) {
        log.info("[SMS simulé -> {}] {} : {}", masquer(n.getDestinataire().getTelephone()), n.getTitre(), n.getMessage());
    }

    private static String masquer(String tel) {
        return tel.length() < 4 ? "****" : "*****" + tel.substring(tel.length() - 4);
    }
}
