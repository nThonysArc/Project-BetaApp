package pe.betaagroindustrial.avance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Security security) {

    public record Cors(String allowedOrigins) {}

    public record Security(Jwt jwt) {
        public record Jwt(String secret, long expirationMs) {}
    }
}