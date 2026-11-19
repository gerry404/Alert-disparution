package cm.alerte.disparition;

import cm.alerte.disparition.commun.LimiteurDebit;
import cm.alerte.disparition.securite.SessionService;
import cm.alerte.disparition.utilisateur.Role;
import cm.alerte.disparition.utilisateur.Utilisateur;
import cm.alerte.disparition.utilisateur.UtilisateurRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les scenarios de la section 10 du cahier des charges (CA01 a CA15),
 * joues de bout en bout sur l'API avec une vraie base (H2 + migrations Flyway).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CriteresAcceptationTest {

    // ids poses par la migration V2
    static final long AKWA = 1, DEIDO = 4, NDOKOTI = 8, LOGBABA = 9, MAKEPE = 15;
    static final long CAT_TELEPHONE = 1, CAT_CNI = 4;

    private static final AtomicInteger compteur = new AtomicInteger(10_000_000);

    @Autowired MockMvc mvc;
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired SessionService sessions;
    @Autowired LimiteurDebit limiteur;

    String famille;
    String moderateur;

    @BeforeEach
    void preparer() throws Exception {
        limiteur.reinitialiser();
        famille = inscrire();
        moderateur = compte(Role.MODERATEUR);
    }

    // ------------------------------------------------------------ consultation

    @Test
    @DisplayName("CA01 - un visiteur consulte les dossiers sans compte")
    void ca01() throws Exception {
        mvc.perform(get("/api/dossiers")).andExpect(status().isOk()).andExpect(jsonPath("$.contenu").isArray());
        mvc.perform(get("/api/referentiel/quartiers")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(20)));
    }

    @Test
    @DisplayName("CA02 / CA03 - soumis = en attente et invisible ; valide = public")
    void ca02_ca03() throws Exception {
        long id = signalerPersonne(famille, "Disparition de Jean Mbappe", 40, NDOKOTI);
        String slug = slug(famille, id);

        mvc.perform(get("/api/dossiers/" + slug)).andExpect(status().isNotFound());
        assertThat(referencesPubliques()).doesNotContain(slug);

        decider(id, "publier", null).andExpect(status().isOk()).andExpect(jsonPath("$.infos.statut").value("PUBLIE"));

        mvc.perform(get("/api/dossiers/" + slug)).andExpect(status().isOk())
                .andExpect(jsonPath("$.lieuPrecis").doesNotExist())
                .andExpect(jsonPath("$.telephoneDeclarant").doesNotExist());
        assertThat(referencesPubliques()).contains(slug);
    }

    @Test
    @DisplayName("CA04 - un dossier rejete n'est pas public, et le rejet exige un motif")
    void ca04() throws Exception {
        long id = signalerPersonne(famille, "Avis douteux", 30, AKWA);

        decider(id, "rejeter", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MOTIF_OBLIGATOIRE"));
        decider(id, "rejeter", "Informations incohérentes").andExpect(status().isOk());

        mvc.perform(get("/api/dossiers/" + slug(famille, id))).andExpect(status().isNotFound());
        decider(id, "publier", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSITION_INTERDITE"));
    }

    @Test
    @DisplayName("RG05 - un mineur n'apparait jamais publiquement, meme valide")
    void mineur() throws Exception {
        long id = signalerPersonne(famille, "Enfant disparu", 9, NDOKOTI);
        decider(id, "publier", null).andExpect(status().isOk()).andExpect(jsonPath("$.mineur").value(true));

        mvc.perform(get("/api/dossiers/" + slug(famille, id))).andExpect(status().isNotFound());
        assertThat(referencesPubliques()).doesNotContain(slug(famille, id));
    }

    // ------------------------------------------------------------- temoignages

    @Test
    @DisplayName("CA05 / CA06 - temoignage sans compte, modere, identite jamais publique")
    void ca05_ca06() throws Exception {
        long id = dossierPublie("Disparition de Rose Ngono", AKWA);

        temoigner(id, "Maurice", true).andExpect(status().isAccepted());
        mvc.perform(get("/api/dossiers/" + id + "/temoignages")).andExpect(jsonPath("$", hasSize(0)));

        long temoignageId = premierTemoignageEnAttente(id);
        mvc.perform(post("/api/moderation/temoignages/" + temoignageId + "/publier").header("Authorization", bearer(moderateur)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/dossiers/" + id + "/temoignages"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].auteur").value("Témoin anonyme"))
                .andExpect(jsonPath("$[0].telephoneTemoin").doesNotExist())
                .andExpect(content().string(not(containsString("Maurice"))));
    }

    @Test
    @DisplayName("Acces a l'identite d'un temoin : motif obligatoire et journalise")
    void identiteTemoin() throws Exception {
        long id = dossierPublie("Disparition de Luc Eto", AKWA);
        temoigner(id, "Samuel", true);
        long t = premierTemoignageEnAttente(id);

        mvc.perform(post("/api/moderation/temoignages/" + t + "/identite").header("Authorization", bearer(moderateur))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/moderation/temoignages/" + t + "/identite").header("Authorization", bearer(moderateur))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Rappeler pour précisions\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nom").value("Samuel"));

        mvc.perform(get("/api/admin/journal-acces").header("Authorization", bearer(compte(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu[0].motif").value("Rappeler pour précisions"));
    }

    @Test
    @DisplayName("Anti-spam : au-dela de la limite horaire, les temoignages sont refuses (429)")
    void limiteTemoignages() throws Exception {
        long id = dossierPublie("Disparition de Anne Biya", AKWA);
        for (int i = 0; i < 3; i++) temoigner(id, null, true).andExpect(status().isAccepted());
        temoigner(id, null, true).andExpect(status().isTooManyRequests());
    }

    // ----------------------------------------------------------------- alertes

    @Test
    @DisplayName("CA07 / CA08 / CA09 - alerte de zone a la publication, pas pour un temoignage")
    void ca07_ca08_ca09() throws Exception {
        String voisin = inscrire();
        mvc.perform(post("/api/moi/alertes").header("Authorization", bearer(voisin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quartierId\":" + LOGBABA + "}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/moi/alertes").header("Authorization", bearer(voisin)))
                .andExpect(jsonPath("$[0].quartier").value("Logbaba"));

        long id = dossierPublie("Disparition de Paul Ekwalla", LOGBABA);
        assertThat(notificationsDe(voisin, "NOUVELLE_DISPARITION")).hasSize(1);
        assertThat(notificationsDe(famille, "NOUVELLE_DISPARITION")).isEmpty(); // pas le declarant lui-meme

        temoigner(id, null, true);
        long t = premierTemoignageEnAttente(id);
        mvc.perform(post("/api/moderation/temoignages/" + t + "/publier").header("Authorization", bearer(moderateur)));

        assertThat(notificationsDe(voisin, "NOUVELLE_DISPARITION")).hasSize(1); // CA09 : toujours une seule
        assertThat(notificationsDe(famille, "TEMOIGNAGE_PUBLIE")).hasSize(1);    // la famille, elle, est prevenue
    }

    @Test
    @DisplayName("RG16 - une zone desactivee ne recoit plus d'alerte")
    void alerteDesactivee() throws Exception {
        String voisin = inscrire();
        String json = mvc.perform(post("/api/moi/alertes").header("Authorization", bearer(voisin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quartierId\":" + MAKEPE + "}"))
                .andReturn().getResponse().getContentAsString();
        int abonnement = JsonPath.read(json, "$.id");
        mvc.perform(patch("/api/moi/alertes/" + abonnement).header("Authorization", bearer(voisin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"actif\":false}")).andExpect(status().isOk());

        dossierPublie("Disparition à Makepe", MAKEPE);
        assertThat(notificationsDe(voisin, "NOUVELLE_DISPARITION")).isEmpty();
    }

    // ----------------------------------------------------------- rapprochement

    @Test
    @DisplayName("CA10 - un objet perdu et un objet trouve semblables donnent une correspondance a examiner")
    void ca10() throws Exception {
        String trouveur = inscrire();
        long perdu = signalerObjet(famille, "OBJET_PERDU", "Itel A70 bleu perdu", CAT_TELEPHONE, "Itel", "Bleu", DEIDO,
                "coque rose avec paillettes");
        long trouve = signalerObjet(trouveur, "OBJET_TROUVE", "Téléphone Itel trouvé", CAT_TELEPHONE, "itel", "bleu", DEIDO,
                "coque rose, paillettes");
        decider(perdu, "publier", null);
        decider(trouve, "publier", null);

        String json = mvc.perform(get("/api/moderation/correspondances").header("Authorization", bearer(moderateur)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> liste = JsonPath.read(json, "$[?(@.perdu.id == " + perdu + ")]");
        assertThat(liste).hasSize(1);
        assertThat((Integer) liste.getFirst().get("score")).isGreaterThanOrEqualTo(50);

        int cid = (Integer) liste.getFirst().get("id");
        mvc.perform(post("/api/moderation/correspondances/" + cid + "/valider").header("Authorization", bearer(moderateur)))
                .andExpect(status().isOk());
        assertThat(notificationsDe(famille, "CORRESPONDANCE")).hasSize(1);
        assertThat(notificationsDe(trouveur, "CORRESPONDANCE")).hasSize(1);
    }

    @Test
    @DisplayName("Document sensible : la photo n'est jamais publique")
    void photoSensible() throws Exception {
        long id = mvc.perform(multipart("/api/moi/dossiers")
                        .file(new MockMultipartFile("photo", "cni.png", "image/png", PNG_1X1))
                        .param("type", "OBJET_TROUVE").param("titre", "CNI trouvée").param("categorieId", "" + CAT_CNI)
                        .param("quartierId", "" + DEIDO).param("dateFaits", hier())
                        .header("Authorization", bearer(famille)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(j -> ((Number) JsonPath.read(j, "$.infos.id")).longValue());
        decider(id, "publier", null);
        mvc.perform(get("/api/dossiers/" + slug(famille, id))).andExpect(jsonPath("$.photo").value(nullValue()));
    }

    @Test
    @DisplayName("Upload : un faux fichier image est refuse quel que soit son Content-Type")
    void fausseImage() throws Exception {
        mvc.perform(multipart("/api/moi/dossiers")
                        .file(new MockMultipartFile("photo", "photo.jpg", "image/jpeg", "<?php echo 1; ?>".getBytes()))
                        .param("type", "PERSONNE").param("titre", "Test").param("nomComplet", "Test")
                        .param("quartierId", "" + AKWA).param("dateFaits", hier())
                        .header("Authorization", bearer(famille)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORMAT_IMAGE"));
    }

    // ----------------------------------------------------------- retrouvailles

    @Nested
    @DisplayName("Retrouvailles")
    class Retrouvailles {

        @Test
        @DisplayName("CA11 / CA12 / CA13 - verification, resolution justifiee, reouverture avec historique")
        void cycleComplet() throws Exception {
            long id = dossierPublie("Disparition de Marc Abega", AKWA);

            mvc.perform(post("/api/moi/dossiers/" + id + "/retrouve").header("Authorization", bearer(famille))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"Il est rentré ce matin\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.infos.statut").value("EN_VERIFICATION")); // RG10 : pas resolu

            decider(id, "resoudre", "").andExpect(status().isBadRequest());            // RG11
            decider(id, "resoudre", "Confirmé par téléphone avec la famille").andExpect(status().isOk())
                    .andExpect(jsonPath("$.infos.statut").value("RESOLU"));

            decider(id, "rouvrir", "Nouvelle disparition signalée").andExpect(status().isOk())
                    .andExpect(jsonPath("$.infos.statut").value("PUBLIE"))
                    .andExpect(jsonPath("$.historique", hasSize(5)))
                    .andExpect(jsonPath("$.historique[*].nouveau",
                            contains("EN_ATTENTE", "PUBLIE", "EN_VERIFICATION", "RESOLU", "PUBLIE")));
        }

        @Test
        @DisplayName("Seul le declarant peut declarer son dossier retrouve")
        void autreUtilisateur() throws Exception {
            long id = dossierPublie("Disparition de Eric Fotso", AKWA);
            mvc.perform(post("/api/moi/dossiers/" + id + "/retrouve").header("Authorization", bearer(inscrire()))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"je mens\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("CA14 - un dossier suspendu disparait des resultats publics")
    void ca14() throws Exception {
        long id = dossierPublie("Disparition de Ali Bouba", AKWA);
        String slug = slug(famille, id);
        decider(id, "masquer", "Signalement de harcèlement en cours d'examen").andExpect(status().isOk());

        mvc.perform(get("/api/dossiers/" + slug)).andExpect(status().isNotFound());
        assertThat(referencesPubliques()).doesNotContain(slug);
    }

    @Test
    @DisplayName("CA15 - acces refuse aux donnees privees et a la moderation")
    void ca15() throws Exception {
        mvc.perform(get("/api/moderation/dossiers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/moderation/dossiers").header("Authorization", bearer(famille))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/utilisateurs").header("Authorization", bearer(moderateur))).andExpect(status().isForbidden());
        mvc.perform(get("/api/moi/dossiers").header("Authorization", "Bearer token-invente")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deconnexion : le token ne fonctionne plus")
    void deconnexion() throws Exception {
        String token = inscrire();
        mvc.perform(post("/api/auth/deconnexion").header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/moi").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    // ================================================================ outils

    private static final byte[] PNG_1X1 = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private String inscrire() throws Exception {
        String tel = "6" + compteur.incrementAndGet();
        String json = mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"telephone\":\"" + tel + "\",\"motDePasse\":\"motdepasse\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        limiteur.reinitialiser();
        return JsonPath.read(json, "$.token");
    }

    private String compte(Role role) {
        var u = new Utilisateur(role.name(), null, "6" + compteur.incrementAndGet(), null, "x");
        u.setRole(role);
        return sessions.ouvrir(utilisateurs.save(u));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String hier() {
        return LocalDateTime.now().minusDays(1).withNano(0).toString();
    }

    private long signalerPersonne(String token, String titre, int age, long quartier) throws Exception {
        return creer(multipart("/api/moi/dossiers").param("type", "PERSONNE").param("titre", titre)
                .param("nomComplet", titre).param("age", "" + age).param("quartierId", "" + quartier)
                .param("lieuPrecis", "Derrière l'église").param("dateFaits", hier()), token);
    }

    private long signalerObjet(String token, String type, String titre, long categorie, String marque, String couleur,
                               long quartier, String signes) throws Exception {
        return creer(multipart("/api/moi/dossiers").param("type", type).param("titre", titre)
                .param("categorieId", "" + categorie).param("marque", marque).param("couleur", couleur)
                .param("signesDistinctifs", signes).param("quartierId", "" + quartier).param("dateFaits", hier()), token);
    }

    private long creer(MockMultipartHttpServletRequestBuilder requete, String token) throws Exception {
        String json = mvc.perform(requete.header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.infos.statut").value("EN_ATTENTE"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.infos.id")).longValue();
    }

    private long dossierPublie(String titre, long quartier) throws Exception {
        long id = signalerPersonne(famille, titre, 35, quartier);
        decider(id, "publier", null).andExpect(status().isOk());
        return id;
    }

    private org.springframework.test.web.servlet.ResultActions decider(long id, String decision, String motif) throws Exception {
        MockHttpServletRequestBuilder r = post("/api/moderation/dossiers/" + id + "/" + decision)
                .header("Authorization", bearer(moderateur)).contentType(MediaType.APPLICATION_JSON)
                .content(motif == null ? "{}" : "{\"motif\":\"" + motif + "\"}");
        return mvc.perform(r);
    }

    private String slug(String token, long id) throws Exception {
        String json = mvc.perform(get("/api/moi/dossiers").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        List<String> slugs = JsonPath.read(json, "$[?(@.infos.id == " + id + ")].infos.slug");
        return slugs.getFirst();
    }

    private List<String> referencesPubliques() throws Exception {
        String json = mvc.perform(get("/api/dossiers?taille=50&inclureResolus=true")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.contenu[*].slug");
    }

    private org.springframework.test.web.servlet.ResultActions temoigner(long dossierId, String nom, boolean anonyme) throws Exception {
        String tel = "6" + compteur.incrementAndGet();
        String corps = """
                {"type":"APERCU","dateFaits":"%s","description":"Je l'ai vu près du marché vers midi.",
                 "telephoneTemoin":"%s","nomTemoin":%s,"anonyme":%s}"""
                .formatted(hier(), tel, nom == null ? "null" : "\"" + nom + "\"", anonyme);
        return mvc.perform(post("/api/dossiers/" + dossierId + "/temoignages")
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private long premierTemoignageEnAttente(long dossierId) throws Exception {
        String json = mvc.perform(get("/api/moderation/temoignages").header("Authorization", bearer(moderateur)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[?(@.dossierId == " + dossierId + ")].temoignage.id");
        return ids.getFirst();
    }

    private List<Object> notificationsDe(String token, String type) throws Exception {
        String json = mvc.perform(get("/api/moi/notifications").header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.contenu[?(@.type == '" + type + "')]");
    }

    private static org.springframework.test.web.servlet.result.ContentResultMatchers content() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content();
    }
}
