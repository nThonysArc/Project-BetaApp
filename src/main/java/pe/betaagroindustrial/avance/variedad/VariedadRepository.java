package pe.betaagroindustrial.avance.variedad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariedadRepository extends JpaRepository<Variedad, Long> {

    List<Variedad> findByProductoIdAndActivoTrueOrderByOrdenAsc(Long productoId);

    List<Variedad> findByProductoIdOrderByOrdenAsc(Long productoId);
}
