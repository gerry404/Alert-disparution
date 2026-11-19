package cm.alerte.disparition.config;

import cm.alerte.disparition.model.*;
import cm.alerte.disparition.model.Enums.*;
import cm.alerte.disparition.repository.*;
import cm.alerte.disparition.service.DossierService;
import cm.alerte.disparition.dto.DossierForm;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Remplit la base au premier demarrage : quartiers de Douala, categories,
 * deux comptes de test et quelques dossiers pour que le front ne soit pas vide.
 */
@Component
public class DonneesInitiales implements CommandLineRunner {

    private final RegionRepository regions;
    private final VilleRepository villes;
    private final QuartierRepository quartiers;
    private final CategorieRepository categories;
    private final UtilisateurRepository utilisateurs;
    private final DossierService dossierService;
    private final TemoignageRepository temoignages;
    private final PasswordEncoder encoder;

    public DonneesInitiales(RegionRepository regions, VilleRepository villes, QuartierRepository quartiers,
                            CategorieRepository categories, UtilisateurRepository utilisateurs,
                            DossierService dossierService, TemoignageRepository temoignages,
                            PasswordEncoder encoder) {
        this.regions = regions;
        this.villes = villes;
        this.quartiers = quartiers;
        this.categories = categories;
        this.utilisateurs = utilisateurs;
        this.dossierService = dossierService;
        this.temoignages = temoignages;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (regions.count() > 0) {
            return;
        }

        Region littoral = regions.save(new Region("Littoral"));
        Ville douala = villes.save(new Ville("Douala", littoral));

        String[][] liste = {
                {"Akwa", "Douala 1er"}, {"Bonanjo", "Douala 1er"}, {"Bonapriso", "Douala 1er"},
                {"Deido", "Douala 1er"}, {"Bali", "Douala 1er"},
                {"New Bell", "Douala 2e"}, {"Nkololoun", "Douala 2e"},
                {"Ndokoti", "Douala 3e"}, {"Logbaba", "Douala 3e"}, {"Yassa", "Douala 3e"}, {"PK 14", "Douala 3e"},
                {"Bonabéri", "Douala 4e"}, {"Mabanda", "Douala 4e"},
                {"Bepanda", "Douala 5e"}, {"Makepe", "Douala 5e"}, {"Kotto", "Douala 5e"},
                {"Logpom", "Douala 5e"}, {"Bonamoussadi", "Douala 5e"}, {"Ndogbong", "Douala 5e"},
                {"Manoka", "Douala 6e"}
        };
        for (String[] q : liste) {
            quartiers.save(new Quartier(q[0], q[1], douala));
        }

        categories.saveAll(List.of(
                new Categorie("Téléphone", false),
                new Categorie("Sac / Portefeuille", false),
                new Categorie("Clés", false),
                new Categorie("Carte nationale d'identité", true),
                new Categorie("Passeport", true),
                new Categorie("Carte bancaire", true),
                new Categorie("Documents administratifs", true),
                new Categorie("Ordinateur / Tablette", false),
                new Categorie("Animal", false),
                new Categorie("Autre", false)));

        Utilisateur modo = new Utilisateur();
        modo.setNom("Modérateur");
        modo.setTelephone("600000000");
        modo.setMotDePasse(encoder.encode("moderateur"));
        modo.setRole(Role.MODERATEUR);
        utilisateurs.save(modo);

        Utilisateur famille = new Utilisateur();
        famille.setNom("Ngo Bassa");
        famille.setPrenom("Claire");
        famille.setTelephone("699000000");
        famille.setMotDePasse(encoder.encode("demo1234"));
        utilisateurs.save(famille);

        Dossier d1 = personne(famille, "Disparition de Paul Ekwalla", "Paul Ekwalla", "M", 67,
                "Ndokoti", 3, "Taille moyenne, cheveux blancs, porte des lunettes.",
                "Chemise bleue, pantalon gris, sandales noires.",
                "Sorti pour aller au marché de Ndokoti, n'est pas rentré. Il souffre de pertes de mémoire.");
        publier(d1, modo);

        Dossier d2 = personne(famille, "Recherche Aïcha Moussa", "Aïcha Moussa", "F", 24,
                "Bonabéri", 6, "Teint clair, 1m65, tresses longues.",
                "Cicatrice sur l'avant-bras gauche.",
                "Vue pour la dernière fois au carrefour Ancien Dalip vers 19h.");
        publier(d2, modo);

        Dossier d3 = objet(famille, TypeDossier.OBJET_PERDU, "Tecno Camon noir perdu dans un taxi",
                "Téléphone", "Tecno", "Camon 30", "Noir", "Akwa", 2,
                "Coque transparente avec un autocollant des Lions Indomptables.");
        publier(d3, modo);

        Dossier d4 = objet(famille, TypeDossier.OBJET_TROUVE, "Sac à dos bleu trouvé à la gare voyageurs",
                "Sac / Portefeuille", null, null, "Bleu", "Bali", 1,
                "Contient des cahiers et une trousse.");
        publier(d4, modo);

        Dossier d5 = objet(famille, TypeDossier.OBJET_TROUVE, "CNI trouvée vers le rond-point Deido",
                "Carte nationale d'identité", null, null, null, "Deido", 4,
                "Le nom commence par la lettre M. Le propriétaire devra donner les autres informations.");
        publier(d5, modo);

        // un dossier en attente pour tester la moderation
        objet(famille, TypeDossier.OBJET_PERDU, "Clés de voiture Toyota perdues",
                "Clés", "Toyota", null, "Noir", "Makepe", 1, "Porte-clés en cuir marron.");

        Temoignage t = new Temoignage();
        t.setDossier(d1);
        t.setType(TypeTemoignage.APERCU);
        t.setDateFaits(LocalDateTime.now().minusDays(1).withHour(16).withMinute(30));
        t.setQuartier(quartiers.findAll().stream().filter(q -> q.getNom().equals("Logbaba")).findFirst().orElse(null));
        t.setDescription("Un monsieur âgé qui correspond à la description était assis près de la pharmacie, il semblait perdu.");
        t.setTelephoneTemoin("677000000");
        t.setStatut(StatutTemoignage.PUBLIE);
        t.setValidePar(modo);
        temoignages.save(t);
    }

    private Dossier personne(Utilisateur u, String titre, String nom, String sexe, int age, String quartier,
                             int ilYaJours, String physique, String signes, String circonstances) {
        DossierForm f = new DossierForm();
        f.setType(TypeDossier.PERSONNE);
        f.setTitre(titre);
        f.setNomComplet(nom);
        f.setSexe(sexe);
        f.setAge(age);
        f.setDescriptionPhysique(physique);
        f.setSignesDistinctifs(signes);
        f.setCirconstances(circonstances);
        f.setDateFaits(LocalDateTime.now().minusDays(ilYaJours));
        f.setQuartierId(idQuartier(quartier));
        return dossierService.creer(f, u);
    }

    private Dossier objet(Utilisateur u, TypeDossier type, String titre, String categorie, String marque,
                          String modele, String couleur, String quartier, int ilYaJours, String signes) {
        DossierForm f = new DossierForm();
        f.setType(type);
        f.setTitre(titre);
        f.setMarque(marque);
        f.setModele(modele);
        f.setCouleur(couleur);
        f.setSignesDistinctifs(signes);
        f.setDateFaits(LocalDateTime.now().minusDays(ilYaJours));
        f.setQuartierId(idQuartier(quartier));
        f.setCategorieId(categories.findAll().stream()
                .filter(c -> c.getLibelle().equals(categorie)).findFirst().map(Categorie::getId).orElse(null));
        return dossierService.creer(f, u);
    }

    private void publier(Dossier d, Utilisateur modo) {
        dossierService.changerStatut(d, StatutDossier.PUBLIE, modo, "Validé (données de démo)");
    }

    private Long idQuartier(String nom) {
        return quartiers.findAll().stream().filter(q -> q.getNom().equals(nom)).findFirst().orElseThrow().getId();
    }
}
