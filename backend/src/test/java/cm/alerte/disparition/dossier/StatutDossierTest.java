package cm.alerte.disparition.dossier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static cm.alerte.disparition.dossier.StatutDossier.*;
import static org.assertj.core.api.Assertions.assertThat;

class StatutDossierTest {

    @ParameterizedTest(name = "{0} -> {1} : {2}")
    @CsvSource({
            "EN_ATTENTE, PUBLIE, true",
            "EN_ATTENTE, REJETE, true",
            "EN_ATTENTE, RESOLU, false",       // on ne resout pas un dossier jamais publie
            "PUBLIE, EN_VERIFICATION, true",
            "PUBLIE, REJETE, false",
            "EN_VERIFICATION, RESOLU, true",
            "EN_VERIFICATION, PUBLIE, true",   // retrouvaille non confirmee
            "RESOLU, PUBLIE, true",            // reouverture (RG13)
            "RESOLU, EN_ATTENTE, false",
            "REJETE, PUBLIE, false",           // un rejet est definitif
    })
    void transitions(StatutDossier depuis, StatutDossier vers, boolean autorisee) {
        assertThat(depuis.peutPasserA(vers)).isEqualTo(autorisee);
    }

    @Test
    void seulsLesStatutsValidesSontPublics() {
        assertThat(EN_ATTENTE.estPublic()).isFalse();
        assertThat(REJETE.estPublic()).isFalse();
        assertThat(PUBLIE.estPublic()).isTrue();
        assertThat(EN_VERIFICATION.estPublic()).isTrue();
        assertThat(RESOLU.estPublic()).isTrue();
    }
}
