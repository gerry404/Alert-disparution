package cm.alerte.disparition.utilisateur;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class UtilisateurDtos {

    /** 6XXXXXXXX, avec ou sans +237 / 00237 / 237 devant, espaces ou points autorises. */
    public static final String REGEX_TEL = "^(\\+?237|00237)?[\\s.-]?6([\\s.-]?\\d){8}$";

    private UtilisateurDtos() {
    }

    public record InscriptionRequest(
            @NotBlank @Size(max = 100) String nom,
            @Size(max = 100) String prenom,
            @NotBlank @Pattern(regexp = REGEX_TEL, message = "Numéro camerounais attendu (6XX XX XX XX)") String telephone,
            @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72, message = "8 caractères minimum") String motDePasse) {
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

    public record UtilisateurAdminDto(Long id, String nom, String telephone, Role role, boolean actif,
                                      LocalDateTime creeLe) {
        public static UtilisateurAdminDto de(Utilisateur u) {
            return new UtilisateurAdminDto(u.getId(), u.nomAffiche(), u.getTelephone(), u.getRole(), u.isActif(), u.getCreeLe());
        }
    }

    public record ModificationCompteRequest(Role role, Boolean actif) {
    }
}
