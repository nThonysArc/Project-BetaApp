package pe.betaagroindustrial.avance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import pe.betaagroindustrial.avance.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AvanceProduccionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvanceProduccionApplication.class, args);
    }
}