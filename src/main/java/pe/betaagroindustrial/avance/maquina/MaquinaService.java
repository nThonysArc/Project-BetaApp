package pe.betaagroindustrial.avance.maquina;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.campana.Campana;
import pe.betaagroindustrial.avance.campana.CampanaRepository;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaRequest;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaResponse;
import pe.betaagroindustrial.avance.usuario.Usuario;
import pe.betaagroindustrial.avance.usuario.UsuarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final CampanaRepository campanaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<MaquinaResponse> listarPorCampana(Long campanaId) {
        return maquinaRepository.findByCampanaIdAndActivoTrueOrderByOrdenAsc(campanaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaquinaResponse obtenerPorId(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public MaquinaResponse crear(MaquinaRequest request) {
        Campana campana = campanaRepository.findById(request.campanaId())
                .orElseThrow(() -> ResourceNotFoundException.of("Campana", request.campanaId()));

        Usuario supervisor = resolverSupervisor(request.supervisorId());

        Maquina maquina = Maquina.builder()
                .campana(campana)
                .nombre(request.nombre())
                .supervisor(supervisor)
                .orden(request.orden() != null ? request.orden() : 0)
                .activo(true)
                .build();

        return toResponse(maquinaRepository.save(maquina));
    }

    @Transactional
    public MaquinaResponse actualizar(Long id, MaquinaRequest request) {
        Maquina maquina = buscarOFallar(id);

        if (!maquina.getCampana().getId().equals(request.campanaId())) {
            Campana campana = campanaRepository.findById(request.campanaId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Campana", request.campanaId()));
            maquina.setCampana(campana);
        }

        maquina.setNombre(request.nombre());
        maquina.setSupervisor(resolverSupervisor(request.supervisorId()));
        maquina.setOrden(request.orden() != null ? request.orden() : maquina.getOrden());

        return toResponse(maquinaRepository.save(maquina));
    }

    @Transactional
    public void desactivar(Long id) {
        Maquina maquina = buscarOFallar(id);
        maquina.setActivo(false);
        maquinaRepository.save(maquina);
    }

    private Usuario resolverSupervisor(Long supervisorId) {
        if (supervisorId == null) {
            return null;
        }
        return usuarioRepository.findById(supervisorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario (supervisor)", supervisorId));
    }

    private Maquina buscarOFallar(Long id) {
        return maquinaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Maquina", id));
    }

    private MaquinaResponse toResponse(Maquina maquina) {
        return new MaquinaResponse(
                maquina.getId(),
                maquina.getCampana().getId(),
                maquina.getNombre(),
                maquina.getSupervisor() != null ? maquina.getSupervisor().getId() : null,
                maquina.getSupervisor() != null ? maquina.getSupervisor().getNombreCompleto() : null,
                maquina.getOrden(),
                maquina.isActivo()
        );
    }
}
