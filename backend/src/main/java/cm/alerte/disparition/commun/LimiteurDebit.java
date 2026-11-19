package cm.alerte.disparition.commun;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation par fenetre glissante, en memoire.
 * Suffisant pour une seule instance ; avec plusieurs instances il faudra
 * passer sur Redis (ou equivalent), l'interface ne changera pas.
 */
@Component
public class LimiteurDebit {

    private static final Duration DUREE_MAX = Duration.ofHours(24);

    private final Map<String, Deque<Long>> fenetres = new ConcurrentHashMap<>();
    private final Clock horloge;

    public LimiteurDebit(Clock horloge) {
        this.horloge = horloge;
    }

    /** Leve une erreur 429 si "cle" a deja ete utilisee "max" fois dans la fenetre. */
    public void verifier(String cle, int max, Duration fenetre) {
        long maintenant = horloge.millis();
        Deque<Long> appels = fenetres.computeIfAbsent(cle, k -> new ArrayDeque<>());
        synchronized (appels) {
            while (!appels.isEmpty() && maintenant - appels.peekFirst() >= fenetre.toMillis()) {
                appels.pollFirst();
            }
            if (appels.size() >= max) {
                throw ErreurMetier.tropDeRequetes();
            }
            appels.addLast(maintenant);
        }
    }

    public void reinitialiser() {
        fenetres.clear();
    }

    @Scheduled(fixedDelayString = "PT10M")
    void nettoyer() {
        long limite = horloge.millis() - DUREE_MAX.toMillis();
        fenetres.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long dernier = e.getValue().peekLast();
                return dernier == null || dernier < limite;
            }
        });
    }
}
