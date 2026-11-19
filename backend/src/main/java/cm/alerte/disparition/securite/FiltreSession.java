package cm.alerte.disparition.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * "Authorization: Bearer xxx" -> utilisateur courant.
 * Volontairement pas un @Component : il n'est enregistre que dans la chaine Spring Security.
 */
class FiltreSession extends OncePerRequestFilter {

    private final SessionService sessions;

    FiltreSession(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = tokenDe(req);
        if (token != null) {
            sessions.utilisateurDe(token).ifPresent(u -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        u, token, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(req, res);
    }

    static String tokenDe(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") && header.length() > 7 ? header.substring(7) : null;
    }
}
