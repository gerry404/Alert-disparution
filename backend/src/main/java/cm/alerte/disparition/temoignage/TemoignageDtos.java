package cm.alerte.disparition.temoignage;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class TemoignageDtos {

    private TemoignageDtos() {
    }

    public record TemoignageRequest(
            @NotNull Temoignage.Type type,
            @NotNull @PastOrPresent(message = "La date ne peut pas être dans le futur") LocalDateTime dateFaits,
            Long quartierId,
            @Size(max = 255) String lieuPrecis,
            @NotBlank @Size(min = 10, max = 2000, message = "Entre 10 et 2000 caractères") String description,
            @Size(max = 100) String nomTemoin,
            @NotBlank @Pattern(regexp = "^(\\+237|00237)?[\\s.-]?6([\\s.-]?\\d){8}$",
                    message = "Numéro camerounais attendu (6XX XX XX XX)") String telephoneTemoin,
            boolean anonyme) {
    }

    public record TemoignagePublic(Long id, Temoignage.Type type, LocalDateTime dateFaits, String quartier,
                                   String description, String auteur) {
        static TemoignagePublic de(Temoignage t) {
            return new TemoignagePublic(t.getId(), t.getType(), t.getDateFaits(),
                    t.getQuartier() == null ? null : t.getQuartier().getNom(), t.getDescription(), t.auteurPublic());
        }
    }

    /** Pour la moderation : toujours SANS identite, elle se demande explicitement. */
    public record TemoignageModeration(TemoignagePublic temoignage, boolean anonyme, String lieuPrecis,
                                       Long dossierId, String dossierTitre, String dossierReference,
                                       LocalDateTime dateSoumission) {
        static TemoignageModeration de(Temoignage t) {
            return new TemoignageModeration(TemoignagePublic.de(t), t.isAnonyme(), t.getLieuPrecis(),
                    t.getDossier().getId(), t.getDossier().getTitre(), t.getDossier().getReference(),
                    t.getDateSoumission());
        }
    }

    public record MotifRequest(String motif) {
    }

    public record IdentiteTemoin(String nom, String telephone) {
    }

    public record AccesIdentiteDto(LocalDateTime date, String moderateur, String motif, Long temoignageId,
                                   String dossierReference) {
        static AccesIdentiteDto de(AccesIdentite a) {
            return new AccesIdentiteDto(a.getDate(), a.getModerateur().nomAffiche(), a.getMotif(),
                    a.getTemoignage().getId(), a.getTemoignage().getDossier().getReference());
        }
    }

    /** Publie apres validation d'un temoignage : le declarant sera prevenu. */
    public record TemoignagePublie(Long dossierId, Long temoignageId) {
    }
}
