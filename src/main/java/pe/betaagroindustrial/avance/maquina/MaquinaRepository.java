package pe.betaagroindustrial.avance.maquina;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    List<Maquina> findByCampanaIdAndActivoTrueOrderByOrdenAsc(Long campanaId);
}
