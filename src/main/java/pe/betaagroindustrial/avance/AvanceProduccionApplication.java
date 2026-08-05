package pe.betaagroindustrial.avance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import pe.betaagroindustrial.avance.config.AppProperties;

/**
 * Punto de entrada de la aplicacion.
 *
 * Sistema de registro de avance de produccion por hora - Beta Agroindustrial.
 * MVP Fase 1: alcance inicial arandano, arquitectura preparada para
 * multi-producto (esparrago, uva, granada) sin cambios de esquema.
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AvanceProduccionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvanceProduccionApplication.class, args);
    }
}
