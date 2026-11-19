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

    /**
     * Variante "compter seulement les echecs" (connexion) : on refuse si trop d'echecs recents,
     * sans consommer de jeton ; {@link #enregistrerEchec} et {@link #effacer} gerent le compteur.
     */
    public void exigerNonBloque(String cle, int maxEchecs, Duration fenetre) {
        Deque<Long> appels = fenetres.get(cle);
        if (appels == null) return;
        long maintenant = horloge.millis();
        synchronized (appels) {
            while (!appels.isEmpty() && maintenant - appels.peekFirst() >= fenetre.toMillis()) {
                appels.pollFirst();
            }
            if (appels.size() >= maxEchecs) {
                throw ErreurMetier.tropDeRequetes();
            }
        }
    }

    public void enregistrerEchec(String cle) {
        Deque<Long> appels = fenetres.computeIfAbsent(cle, k -> new ArrayDeque<>());
        synchronized (appels) {
            appels.addLast(horloge.millis());
        }
    }

    public void effacer(String cle) {
        fenetres.remove(cle);
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
