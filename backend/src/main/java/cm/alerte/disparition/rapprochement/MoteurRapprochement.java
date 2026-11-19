package cm.alerte.disparition.rapprochement;

import cm.alerte.disparition.commun.Textes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Score de ressemblance entre un objet perdu et un objet trouve (section 5.9).
 *
 * Logique volontairement explicable : chaque point du score correspond a une raison
 * lisible par le moderateur. Une correspondance n'est jamais une preuve (RG09).
 *
 * Classe sans dependance Spring ni JPA : testee unitairement.
 */
public final class MoteurRapprochement {

    /** Ce dont le moteur a besoin, extrait du dossier. */
    public record Signalement(Long categorieId, String marque, String modele, String couleur,
                              Long quartierId, LocalDateTime dateFaits, String texte) {
    }

    public record Evaluation(int score, List<String> raisons) {
    }

    private MoteurRapprochement() {
    }

    public static Optional<Evaluation> evaluer(Signalement perdu, Signalement trouve) {
        // eliminatoires
        if (perdu.categorieId() != null && trouve.categorieId() != null
                && !perdu.categorieId().equals(trouve.categorieId())) {
            return Optional.empty();
        }
        if (trouve.dateFaits().isBefore(perdu.dateFaits().minusDays(1))) {
            return Optional.empty(); // trouve avant d'avoir ete perdu
        }

        int score = 0;
        List<String> raisons = new ArrayList<>();

        if (perdu.categorieId() != null && perdu.categorieId().equals(trouve.categorieId())) {
            score += 35;
            raisons.add("même catégorie");
        }

        String m1 = Textes.normaliser(perdu.marque()), m2 = Textes.normaliser(trouve.marque());
        if (!m1.isEmpty() && !m2.isEmpty()) {
            if (m1.equals(m2)) {
                score += 20;
                raisons.add("même marque");
            } else {
                score -= 20;
            }
        }

        String mo1 = Textes.normaliser(perdu.modele()), mo2 = Textes.normaliser(trouve.modele());
        if (!mo1.isEmpty() && !mo2.isEmpty() && (mo1.contains(mo2) || mo2.contains(mo1))) {
            score += 10;
            raisons.add("modèle proche");
        }

        String c1 = premierMot(perdu.couleur()), c2 = premierMot(trouve.couleur());
        if (!c1.isEmpty() && !c2.isEmpty()) {
            if (c1.equals(c2)) {
                score += 15;
                raisons.add("même couleur");
            } else {
                score -= 10;
            }
        }

        if (perdu.quartierId() != null && perdu.quartierId().equals(trouve.quartierId())) {
            score += 10;
            raisons.add("même quartier");
        }

        long jours = Math.abs(Duration.between(perdu.dateFaits(), trouve.dateFaits()).toDays());
        if (jours <= 3) {
            score += 10;
            raisons.add("dates très proches");
        } else if (jours <= 14) {
            score += 5;
            raisons.add("dates proches");
        }

        Set<String> communs = new HashSet<>(Textes.motsSignificatifs(perdu.texte()));
        communs.retainAll(Textes.motsSignificatifs(trouve.texte()));
        if (!communs.isEmpty()) {
            score += Math.min(communs.size(), 3) * 5;
            raisons.add("détails communs : " + String.join(", ", communs.stream().sorted().limit(3).toList()));
        }

        return Optional.of(new Evaluation(Math.clamp(score, 0, 100), raisons));
    }

    private static String premierMot(String s) {
        String n = Textes.normaliser(s);
        int i = n.indexOf(' ');
        return i < 0 ? n : n.substring(0, i);
    }
}
