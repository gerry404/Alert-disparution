package cm.alerte.disparition.dossier;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public final class DossierDtos {

    private DossierDtos() {
    }

    /** Vue publique : ni declarant, ni lieu precis, ni photo d'un document sensible. */
    public record DossierPublic(
            Long id, String reference, String slug, TypeDossier type, StatutDossier statut,
            String titre, String description, LocalDateTime dateFaits, LocalDateTime datePublication,
            Long quartierId, String quartier, String ville, String categorie, String photo,
            String nomComplet, String sexe, Integer age, String descriptionPhysique, String circonstances,
            String marque, String modele, String couleur, String signesDistinctifs) {

        public static DossierPublic de(Dossier d) {
            return new DossierPublic(
                    d.getId(), d.getReference(), d.getSlug(), d.getType(), d.getStatut(),
                    d.getTitre(), d.getDescription(), d.getDateFaits(), d.getDatePublication(),
                    d.getQuartier().getId(), d.getQuartier().getNom(), d.getQuartier().getVille().getNom(),
                    d.getCategorie() == null ? null : d.getCategorie().getLibelle(),
                    d.photoPubliable() ? d.getPhoto() : null,
                    d.getNomComplet(), d.getSexe(), d.getAge(), d.getDescriptionPhysique(), d.getCirconstances(),
                    d.getMarque(), d.getModele(), d.getCouleur(), d.getSignesDistinctifs());
        }
    }

    public record HistoriqueDto(StatutDossier ancien, StatutDossier nouveau, String motif, String auteur,
                                LocalDateTime date) {
        static HistoriqueDto de(HistoriqueStatut h) {
            return new HistoriqueDto(h.getAncienStatut(), h.getNouveauStatut(), h.getMotif(),
                    h.getAuteur() == null ? null : h.getAuteur().nomAffiche(), h.getDate());
        }
    }

    /** Vue declarant / moderation. */
    public record DossierPrive(DossierPublic infos, String lieuPrecis, boolean masque, boolean mineur,
                               String declarant, String telephoneDeclarant, LocalDateTime dateSoumission,
                               long version, List<HistoriqueDto> historique) {

        public static DossierPrive de(Dossier d) {
            return new DossierPrive(DossierPublic.de(d), d.getLieuPrecis(), d.isMasque(), d.estMineur(),
                    d.getDeclarant().nomAffiche(), d.getDeclarant().getTelephone(), d.getDateSoumission(),
                    d.getVersion(), d.getHistorique().stream().map(HistoriqueDto::de).toList());
        }
    }

    public record DecisionRequest(String motif) {
    }

    public record StatsDto(long recherchesActives, long dossiersResolus) {
    }

    /** Formulaire de signalement, envoye en multipart a cause de la photo. */
    @Data
    public static class DossierForm {

        @NotNull
        private TypeDossier type;

        @NotBlank
        @Size(max = 120)
        private String titre;

        @Size(max = 2000)
        private String description;

        @NotNull
        @PastOrPresent(message = "La date ne peut pas être dans le futur")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime dateFaits;

        @NotNull
        private Long quartierId;

        private Long categorieId;

        @Size(max = 255)
        private String lieuPrecis;

        @Size(max = 150)
        private String nomComplet;

        @Pattern(regexp = "^[MF]?$")
        private String sexe;

        @Min(0)
        @Max(120)
        private Integer age;

        @Size(max = 1000)
        private String descriptionPhysique;

        @Size(max = 2000)
        private String circonstances;

        @Size(max = 80)
        private String marque;

        @Size(max = 80)
        private String modele;

        @Size(max = 40)
        private String couleur;

        @Size(max = 1000)
        private String signesDistinctifs;

        private MultipartFile photo;
    }
}
