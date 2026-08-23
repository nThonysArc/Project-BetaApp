package pe.betaagroindustrial.avance.campana;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampanaRepository extends JpaRepository<Campana, Long> {

    Optional<Campana> findByProductoIdAndActivaTrue(Long productoId);

    List<Campana> findByActivaTrue();
}
