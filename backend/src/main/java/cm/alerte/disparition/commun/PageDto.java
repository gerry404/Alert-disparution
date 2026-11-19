package cm.alerte.disparition.commun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Page de resultats, sans exposer la structure interne de Spring Data. */
public record PageDto<T>(List<T> contenu, int page, int taille, long total, int totalPages) {

    public static <E, T> PageDto<T> de(Page<E> page, Function<E, T> conversion) {
        return new PageDto<>(page.getContent().stream().map(conversion).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static int borner(int taille) {
        return Math.clamp(taille, 1, 50);
    }
}
