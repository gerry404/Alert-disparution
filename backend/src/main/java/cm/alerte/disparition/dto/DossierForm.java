package cm.alerte.disparition.dto;

import cm.alerte.disparition.model.Enums.TypeDossier;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Formulaire de signalement (envoye en multipart a cause de la photo).
 */
@Data
public class DossierForm {

    @NotNull
    private TypeDossier type;

    @NotBlank
    @Size(max = 120)
    private String titre;

    @Size(max = 2000)
    private String description;

    @NotNull
    @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFaits;

    @NotNull
    private Long quartierId;

    private Long categorieId;
    private String lieuPrecis;

    // personne
    private String nomComplet;
    private String sexe;
    @Min(0)
    @Max(120)
    private Integer age;
    private String descriptionPhysique;
    private String circonstances;

    // objet
    private String marque;
    private String modele;
    private String couleur;

    private String signesDistinctifs;

    private MultipartFile photo;
}
