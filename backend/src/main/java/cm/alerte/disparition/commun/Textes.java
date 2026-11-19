package cm.alerte.disparition.commun;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class Textes {

    private static final Set<String> MOTS_VIDES = Set.of(
            "avec", "dans", "pour", "sans", "sous", "chez", "vers", "elle", "elles", "leur", "leurs",
            "cette", "celui", "celle", "ainsi", "aussi", "plus", "tres", "tout", "tous", "toute", "toutes",
            "etait", "sont", "avoir", "etre", "fait", "comme", "noir", "noire", "blanc", "blanche");

    private Textes() {
    }

    public static String sansAccents(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static String normaliser(String texte) {
        return texte == null ? "" : sansAccents(texte).toLowerCase(Locale.ROOT).trim();
    }

    public static String slug(String texte) {
        String s = normaliser(texte).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    /** Mots d'au moins 4 lettres, sans accents ni mots vides : sert au rapprochement d'objets. */
    public static Set<String> motsSignificatifs(String... textes) {
        return Arrays.stream(textes)
                .map(Textes::normaliser)
                .flatMap(t -> Arrays.stream(t.split("[^a-z0-9]+")))
                .filter(m -> m.length() >= 4 && !MOTS_VIDES.contains(m))
                .collect(Collectors.toSet());
    }

    /** "6 99 11 22 33", "+237699112233" et "00237 699112233" donnent "699112233". */
    public static String telephone(String brut) {
        String t = brut.replaceAll("[\\s.-]", "");
        if (t.startsWith("+237")) t = t.substring(4);
        else if (t.startsWith("00237")) t = t.substring(5);
        return t;
    }

    public static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
