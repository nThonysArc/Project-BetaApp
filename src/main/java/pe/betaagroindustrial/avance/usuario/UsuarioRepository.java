package pe.betaagroindustrial.avance.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByRolAndActivoTrueOrderByNombreCompletoAsc(Rol rol);

    List<Usuario> findByRolOrderByNombreCompletoAsc(Rol rol);
}
