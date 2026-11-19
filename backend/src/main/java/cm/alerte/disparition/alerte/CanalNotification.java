package cm.alerte.disparition.alerte;

/**
 * Canal de diffusion d'une notification deja enregistree (section 5.8 :
 * "le mecanisme de notification devra etre independant du canal").
 *
 * Ajouter le SMS ou WhatsApp = ecrire une nouvelle implementation, rien d'autre.
 */
public interface CanalNotification {

    String nom();

    void envoyer(Notification notification);
}
