package cm.alerte.disparition.dto;

import cm.alerte.disparition.model.*;
import cm.alerte.disparition.model.Enums.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tout ce qui entre et sort de l'API. On n'expose jamais directement les entites
 * pour ne pas laisser fuiter un telephone ou un lieu precis par erreur.
 */
public final class Dto {

    private Dto() {
    }

    // ---------- Auth ----------

    public record InscriptionRequest(
            @NotBlank String nom,
            String prenom,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ]{9,20}$", message = "Numero invalide") String telephone,
            @Email String email,
            @NotBlank @Size(min = 6, message = "6 caracteres minimum") String motDePasse) {
    }

    public record ConnexionRequest(@NotBlank String telephone, @NotBlank String motDePasse) {
    }

    public record UtilisateurDto(Long id, String nom, String prenom, String telephone, String email, Role role) {
        public static UtilisateurDto de(Utilisateur u) {
            return new UtilisateurDto(u.getId(), u.getNom(), u.getPrenom(), u.getTelephone(), u.getEmail(), u.getRole());
        }
    }

    public record SessionDto(String token, UtilisateurDto utilisateur) {
    }

    // ---------- Referentiel ----------

    public record QuartierDto(Long id, String nom, String arrondissement, String ville) {
        public static QuartierDto de(Quartier q) {
            return new QuartierDto(q.getId(), q.getNom(), q.getArrondissement(), q.getVille().getNom());
        }
    }

    public record CategorieDto(Long id, String libelle, boolean sensible) {
        public static CategorieDto de(Categorie c) {
            return new CategorieDto(c.getId(), c.getLibelle(), c.isSensible());
        }
    }

    public record StatsDto(long dossiersPublies, long dossiersResolus, long temoignagesPublies) {
    }

    // ---------- Dossiers ----------

    /** Ce que voit le public : pas de declarant, pas de lieu precis. */
    public record DossierPublic(
            Long id, String reference, String slug, TypeDossier type, StatutDossier statut,
            String titre, String description, LocalDateTime dateFaits, LocalDateTime datePublication,
            String quartier, String ville, String categorie, String photo,
            String nomComplet, String sexe, Integer age, String descriptionPhysique, String circonstances,
            String marque, String modele, String couleur, String signesDistinctifs) {

        public static DossierPublic de(Dossier d) {
            boolean photoCachee = d.getCategorie() != null && d.getCategorie().isSensible();
            return new DossierPublic(
                    d.getId(), d.getReference(), d.getSlug(), d.getType(), d.getStatut(),
                    d.getTitre(), d.getDescription(), d.getDateFaits(), d.getDatePublication(),
                    d.getQuartier().getNom(), d.getQuartier().getVille().getNom(),
                    d.getCategorie() == null ? null : d.getCategorie().getLibelle(),
                    photoCachee ? null : d.getPhoto(),
                    d.getNomComplet(), d.getSexe(), d.getAge(), d.getDescriptionPhysique(), d.getCirconstances(),
                    d.getMarque(), d.getModele(), d.getCouleur(), d.getSignesDistinctifs());
        }
    }

    public record FicheDossier(DossierPublic dossier, List<TemoignagePublic> temoignages) {
    }

    /** Vue declarant / moderation : on ajoute les infos privees. */
    public record DossierPrive(DossierPublic infos, String lieuPrecis, boolean masque, boolean mineur,
                               String declarant, String telephoneDeclarant, LocalDateTime dateSoumission,
                               List<HistoriqueDto> historique) {
    }

    public record HistoriqueDto(StatutDossier ancien, StatutDossier nouveau, String motif, String auteur,
                                LocalDateTime date) {
        public static HistoriqueDto de(HistoriqueStatut h) {
            return new HistoriqueDto(h.getAncienStatut(), h.getNouveauStatut(), h.getMotif(),
                    h.getAuteur() == null ? null : h.getAuteur().getNomAffiche(), h.getDate());
        }
    }

    public record MotifRequest(String motif) {
    }

    // ---------- Temoignages ----------

    public record TemoignageRequest(
            @NotNull TypeTemoignage type,
            @NotNull @PastOrPresent LocalDateTime dateFaits,
            Long quartierId,
            String lieuPrecis,
            @NotBlank @Size(min = 10, max = 2000) String description,
            String nomTemoin,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ]{9,20}$", message = "Numero invalide") String telephoneTemoin,
            boolean anonyme) {
    }

    /** Version publique : le nom n'apparait que si le temoin a refuse l'anonymat. */
    public record TemoignagePublic(Long id, TypeTemoignage type, LocalDateTime dateFaits, String quartier,
                                   String description, String auteur, LocalDateTime dateSoumission) {
        public static TemoignagePublic de(Temoignage t) {
            String auteur = (t.isAnonyme() || t.getNomTemoin() == null || t.getNomTemoin().isBlank())
                    ? "Témoin anonyme" : t.getNomTemoin();
            return new TemoignagePublic(t.getId(), t.getType(), t.getDateFaits(),
                    t.getQuartier() == null ? null : t.getQuartier().getNom(),
                    t.getDescription(), auteur, t.getDateSoumission());
        }
    }

    /** Pour la moderation : toujours sans identite (il faut la demander explicitement). */
    public record TemoignageModeration(TemoignagePublic temoignage, boolean anonyme, String lieuPrecis,
                                       String dossierTitre, String dossierReference) {
    }

    public record IdentiteTemoin(String nom, String telephone) {
    }
}
