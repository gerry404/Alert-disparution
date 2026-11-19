package cm.alerte.disparition.config;

import cm.alerte.disparition.alerte.AbonnementService;
import cm.alerte.disparition.dossier.DecisionModeration;
import cm.alerte.disparition.dossier.DossierDtos.DossierForm;
import cm.alerte.disparition.dossier.DossierService;
import cm.alerte.disparition.dossier.TypeDossier;
import cm.alerte.disparition.referentiel.Categorie;
import cm.alerte.disparition.referentiel.CategorieRepository;
import cm.alerte.disparition.referentiel.Quartier;
import cm.alerte.disparition.referentiel.QuartierRepository;
import cm.alerte.disparition.temoignage.Temoignage;
import cm.alerte.disparition.temoignage.TemoignageDtos.TemoignageRequest;
import cm.alerte.disparition.temoignage.TemoignageService;
import cm.alerte.disparition.utilisateur.Role;
import cm.alerte.disparition.utilisateur.Utilisateur;
import cm.alerte.disparition.utilisateur.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Jeu de demonstration (app.donnees-demo=true). Les donnees de reference
 * (quartiers, categories) viennent des migrations Flyway, pas d'ici.
 *
 * Tout passe par les services : les evenements (alertes, rapprochement)
 * se declenchent comme en vrai.
 */
@Component
@ConditionalOnProperty(name = "app.donnees-demo", havingValue = "true")
class DonneesDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DonneesDemo.class);

    private final UtilisateurRepository utilisateurs;
    private final QuartierRepository quartiers;
    private final CategorieRepository categories;
    private final DossierService dossiers;
    private final TemoignageService temoignages;
    private final AbonnementService abonnements;
    private final PasswordEncoder encodeur;

    DonneesDemo(UtilisateurRepository utilisateurs, QuartierRepository quartiers, CategorieRepository categories,
                DossierService dossiers, TemoignageService temoignages, AbonnementService abonnements,
                PasswordEncoder encodeur) {
        this.utilisateurs = utilisateurs;
        this.quartiers = quartiers;
        this.categories = categories;
        this.dossiers = dossiers;
        this.temoignages = temoignages;
        this.abonnements = abonnements;
        this.encodeur = encodeur;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurs.count() > 0) return;
        log.info("Base vide : chargement des données de démonstration");

        Utilisateur admin = compte("Admin", null, "600000001", "admin1234", Role.ADMIN);
        Utilisateur modo = compte("Modérateur", null, "600000000", "moderateur", Role.MODERATEUR);
        Utilisateur claire = compte("Ngo Bassa", "Claire", "699000000", "demo1234", Role.DECLARANT);
        Utilisateur yves = compte("Ateba", "Yves", "677000000", "demo1234", Role.DECLARANT);

        // Yves suit Ndokoti et Logbaba : il recevra l'alerte pour Paul
        abonnements.ajouter(yves, quartier("Ndokoti").getId());
        abonnements.ajouter(yves, quartier("Logbaba").getId());

        var paul = personne(claire, "Disparition de Paul Ekwalla", "Paul Ekwalla", "M", 67, "Ndokoti", 3,
                "Taille moyenne, cheveux blancs, lunettes à monture dorée. Chemise bleue, pantalon gris.",
                "Boite légèrement de la jambe droite.",
                "Parti vers 10h pour le marché de Ndokoti, il n'est pas rentré. Il a des pertes de mémoire.");
        publier(paul, modo);

        var aicha = personne(yves, "Recherche Aïcha Moussa", "Aïcha Moussa", "F", 24, "Bonabéri", 6,
                "Teint clair, environ 1m65, longues tresses.", "Cicatrice sur l'avant-bras gauche.",
                "Vue pour la dernière fois au carrefour Ancien Dalip vers 19h.");
        publier(aicha, modo);

        // couple perdu / trouve qui doit sortir en correspondance
        var perdu = objet(claire, TypeDossier.OBJET_PERDU, "Tecno Camon 30 noir oublié dans un taxi", "Téléphone",
                "Tecno", "Camon 30", "Noir", "Akwa", 2, "Coque transparente, autocollant des Lions Indomptables au dos.");
        publier(perdu, modo);
        var trouve = objet(yves, TypeDossier.OBJET_TROUVE, "Téléphone Tecno trouvé près du marché Sandaga", "Téléphone",
                "Tecno", "Camon", "Noir", "Akwa", 1, "Autocollant des Lions Indomptables, écran fissuré en bas.");
        publier(trouve, modo);

        var sac = objet(claire, TypeDossier.OBJET_TROUVE, "Sac à dos bleu trouvé à la gare voyageurs",
                "Sac / Portefeuille", null, null, "Bleu", "Bali", 1, "Contient des cahiers d'écolier et une trousse rouge.");
        publier(sac, modo);

        var cni = objet(yves, TypeDossier.OBJET_TROUVE, "CNI trouvée vers le rond-point Deido",
                "Carte nationale d'identité", null, null, null, "Deido", 4,
                "Nom commençant par M. Le propriétaire devra donner les autres informations.");
        publier(cni, modo);

        objet(yves, TypeDossier.OBJET_PERDU, "Clés de voiture Toyota perdues", "Clés", "Toyota", null, "Noir",
                "Makepe", 1, "Porte-clés en cuir marron avec une petite lampe.");

        temoignages.soumettre(paul, new TemoignageRequest(Temoignage.Type.APERCU,
                LocalDateTime.now().minusDays(1).withHour(16).withMinute(30), quartier("Logbaba").getId(),
                "Devant la pharmacie du carrefour",
                "Un monsieur âgé qui correspond à la description était assis, il avait l'air perdu.",
                null, "677112233", true), "demo");
        temoignages.enAttente().stream().findFirst()
                .ifPresent(t -> temoignages.traiter(t.temoignage().id(), Temoignage.Statut.PUBLIE, modo));

        temoignages.soumettre(aicha, new TemoignageRequest(Temoignage.Type.INFORMATION,
                LocalDateTime.now().minusDays(2), null, null,
                "Je crois l'avoir vue monter dans un car pour Yaoundé à l'agence de Bonabéri.",
                "Samuel", "690445566", false), "demo");

        log.info("Démo prête : admin 600000001/admin1234, modérateur 600000000/moderateur, "
                + "familles 699000000 et 677000000 / demo1234");
    }

    private Utilisateur compte(String nom, String prenom, String tel, String mdp, Role role) {
        Utilisateur u = new Utilisateur(nom, prenom, tel, null, encodeur.encode(mdp));
        u.setRole(role);
        return utilisateurs.save(u);
    }

    private Long personne(Utilisateur u, String titre, String nom, String sexe, int age, String quartier,
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
        f.setQuartierId(quartier(quartier).getId());
        return dossiers.signaler(f, u).infos().id();
    }

    private Long objet(Utilisateur u, TypeDossier type, String titre, String categorie, String marque,
                       String modele, String couleur, String quartier, int ilYaJours, String signes) {
        DossierForm f = new DossierForm();
        f.setType(type);
        f.setTitre(titre);
        f.setMarque(marque);
        f.setModele(modele);
        f.setCouleur(couleur);
        f.setSignesDistinctifs(signes);
        f.setDateFaits(LocalDateTime.now().minusDays(ilYaJours));
        f.setQuartierId(quartier(quartier).getId());
        f.setCategorieId(categories.findByLibelle(categorie).map(Categorie::getId).orElseThrow());
        return dossiers.signaler(f, u).infos().id();
    }

    private void publier(Long id, Utilisateur modo) {
        dossiers.decider(id, DecisionModeration.PUBLIER, "Vérifié (démo)", modo);
    }

    private Quartier quartier(String nom) {
        return quartiers.findAllByOrderByNomAsc().stream().filter(q -> q.getNom().equals(nom)).findFirst().orElseThrow();
    }
}
