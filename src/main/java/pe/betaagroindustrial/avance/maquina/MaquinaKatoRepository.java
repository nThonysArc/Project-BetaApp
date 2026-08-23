package pe.betaagroindustrial.avance.maquina;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaquinaKatoRepository extends JpaRepository<MaquinaKato, Long> {

    List<MaquinaKato> findByMaquinaIdAndActivoTrueOrderByOrdenAsc(Long maquinaId);

    List<MaquinaKato> findByMaquinaIdOrderByOrdenAsc(Long maquinaId);
}
