package pe.betaagroindustrial.avance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resuelve quien es el usuario "actual" para llenar created_by/updated_by
 * automaticamente en las entidades que extienden Auditable.
 *
 * Asume que el "principal" autenticado expone el id del usuario (Long).
 * Ajustar el cast segun la implementacion final de UserDetails/JWT claims.
 */
@Configuration
public class AuditorAwareConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }

            // TODO: una vez definido el UserDetails/JWT propio, castear al tipo real
            // y extraer el id de usuario. Placeholder seguro por ahora:
            Object principal = authentication.getPrincipal();
            if (principal instanceof pe.betaagroindustrial.avance.security.AuthenticatedUser authUser) {
                return Optional.of(authUser.id());
            }
            return Optional.empty();
        };
    }
}
