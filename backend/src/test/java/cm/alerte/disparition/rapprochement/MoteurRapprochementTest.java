package cm.alerte.disparition.rapprochement;

import cm.alerte.disparition.rapprochement.MoteurRapprochement.Signalement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MoteurRapprochementTest {

    private static final long TELEPHONE = 1L, CLES = 3L;
    private static final long AKWA = 1L, BONABERI = 12L;
    private static final LocalDateTime LUNDI = LocalDateTime.of(2026, 11, 16, 10, 0);

    private static Signalement objet(long categorie, String marque, String couleur, long quartier,
                                     LocalDateTime date, String texte) {
        return new Signalement(categorie, marque, null, couleur, quartier, date, texte);
    }

    @Test
    void memeTelephoneMemeQuartierLeLendemainDonneUnScoreEleve() {
        var perdu = objet(TELEPHONE, "Tecno", "Noir", AKWA, LUNDI, "coque transparente autocollant Lions Indomptables");
        var trouve = objet(TELEPHONE, "TECNO", "noir mat", AKWA, LUNDI.plusDays(1), "autocollant des Lions, écran fissuré");

        var e = MoteurRapprochement.evaluer(perdu, trouve).orElseThrow();

        assertThat(e.score()).isGreaterThanOrEqualTo(80);
        assertThat(e.raisons()).contains("même catégorie", "même marque", "même couleur", "même quartier");
        assertThat(e.raisons()).anyMatch(r -> r.startsWith("détails communs") && r.contains("autocollant"));
    }

    @Test
    void categoriesDifferentesNeSontJamaisRapprochees() {
        var perdu = objet(TELEPHONE, "Tecno", "Noir", AKWA, LUNDI, "");
        var trouve = objet(CLES, "Tecno", "Noir", AKWA, LUNDI, "");

        assertThat(MoteurRapprochement.evaluer(perdu, trouve)).isEmpty();
    }

    @Test
    void unObjetTrouveAvantDavoirEtePerduEstElimine() {
        var perdu = objet(TELEPHONE, "Tecno", "Noir", AKWA, LUNDI, "");
        var trouve = objet(TELEPHONE, "Tecno", "Noir", AKWA, LUNDI.minusDays(5), "");

        assertThat(MoteurRapprochement.evaluer(perdu, trouve)).isEmpty();
    }

    @Test
    void marqueEtCouleurDifferentesFontChuterLeScoreSousLeSeuil() {
        var perdu = objet(TELEPHONE, "Tecno", "Noir", AKWA, LUNDI, "");
        var trouve = objet(TELEPHONE, "Samsung", "Blanc", BONABERI, LUNDI.plusDays(20), "");

        var e = MoteurRapprochement.evaluer(perdu, trouve).orElseThrow();
        assertThat(e.score()).isLessThan(50);
    }
}
