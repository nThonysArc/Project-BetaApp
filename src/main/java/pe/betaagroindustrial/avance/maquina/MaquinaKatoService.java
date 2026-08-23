package pe.betaagroindustrial.avance.maquina;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaKatoRequest;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaKatoResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaquinaKatoService {

    private final MaquinaKatoRepository maquinaKatoRepository;
    private final MaquinaRepository maquinaRepository;

    @Transactional(readOnly = true)
    public List<MaquinaKatoResponse> listarPorMaquina(Long maquinaId, boolean incluirInactivos) {
        List<MaquinaKato> katos = incluirInactivos
                ? maquinaKatoRepository.findByMaquinaIdOrderByOrdenAsc(maquinaId)
                : maquinaKatoRepository.findByMaquinaIdAndActivoTrueOrderByOrdenAsc(maquinaId);
        return katos.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MaquinaKatoResponse crear(MaquinaKatoRequest request) {
        Maquina maquina = maquinaRepository.findById(request.maquinaId())
                .orElseThrow(() -> ResourceNotFoundException.of("Maquina", request.maquinaId()));

        MaquinaKato kato = MaquinaKato.builder()
                .maquina(maquina)
                .nombre(request.nombre())
                .orden(request.orden() != null ? request.orden() : 0)
                .activo(true)
                .build();

        return toResponse(maquinaKatoRepository.save(kato));
    }

    @Transactional
    public MaquinaKatoResponse actualizar(Long id, MaquinaKatoRequest request) {
        MaquinaKato kato = buscarOFallar(id);
        kato.setNombre(request.nombre());
        kato.setOrden(request.orden() != null ? request.orden() : kato.getOrden());
        return toResponse(maquinaKatoRepository.save(kato));
    }

    @Transactional
    public void desactivar(Long id) {
        MaquinaKato kato = buscarOFallar(id);
        kato.setActivo(false);
        maquinaKatoRepository.save(kato);
    }

    @Transactional
    public MaquinaKatoResponse reactivar(Long id) {
        MaquinaKato kato = buscarOFallar(id);
        kato.setActivo(true);
        return toResponse(maquinaKatoRepository.save(kato));
    }

    private MaquinaKato buscarOFallar(Long id) {
        return maquinaKatoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("MaquinaKato", id));
    }

    private MaquinaKatoResponse toResponse(MaquinaKato kato) {
        return new MaquinaKatoResponse(
                kato.getId(),
                kato.getMaquina().getId(),
                kato.getNombre(),
                kato.getOrden(),
                kato.isActivo()
        );
    }
}
