package pe.betaagroindustrial.avance.campana;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.campana.dto.CampanaRequest;
import pe.betaagroindustrial.avance.campana.dto.CampanaResponse;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.producto.Producto;
import pe.betaagroindustrial.avance.producto.ProductoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampanaService {

    private final CampanaRepository campanaRepository;
    private final ProductoRepository productoRepository;

    /**
     * Por defecto solo trae campanas activas (comportamiento consistente
     * con Maquina). incluirInactivas=true trae el historial completo,
     * para auditoria o para poder reactivar una campana anterior.
     */
    @Transactional(readOnly = true)
    public List<CampanaResponse> listar(boolean incluirInactivas) {
        List<Campana> campanas = incluirInactivas
                ? campanaRepository.findAll()
                : campanaRepository.findByActivaTrue();
        return campanas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CampanaResponse obtenerPorId(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public CampanaResponse crear(CampanaRequest request) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.productoId()));

        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new BusinessRuleException("La fecha fin no puede ser anterior a la fecha inicio");
        }

        desactivarActivaExistente(producto.getId(), null);

        Campana campana = Campana.builder()
                .producto(producto)
                .nombre(request.nombre())
                .anio(request.anio())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .activa(true)
                .build();

        return toResponse(campanaRepository.save(campana));
    }

    @Transactional
    public CampanaResponse actualizar(Long id, CampanaRequest request) {
        Campana campana = buscarOFallar(id);

        if (!campana.getProducto().getId().equals(request.productoId())) {
            Producto producto = productoRepository.findById(request.productoId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.productoId()));
            campana.setProducto(producto);
        }

        campana.setNombre(request.nombre());
        campana.setAnio(request.anio());
        campana.setFechaInicio(request.fechaInicio());
        campana.setFechaFin(request.fechaFin());

        return toResponse(campanaRepository.save(campana));
    }

    @Transactional
    public void desactivar(Long id) {
        Campana campana = buscarOFallar(id);
        campana.setActiva(false);
        campanaRepository.save(campana);
    }

    /**
     * Reactiva una campana previamente desactivada. Respeta la regla de
     * "1 campana activa por producto": si ya existe otra activa para el
     * mismo producto, la desactiva primero.
     */
    @Transactional
    public CampanaResponse reactivar(Long id) {
        Campana campana = buscarOFallar(id);
        desactivarActivaExistente(campana.getProducto().getId(), campana.getId());
        campana.setActiva(true);
        return toResponse(campanaRepository.save(campana));
    }

    private void desactivarActivaExistente(Long productoId, Long idAExcluir) {
        campanaRepository.findByProductoIdAndActivaTrue(productoId)
                .filter(activa -> idAExcluir == null || !activa.getId().equals(idAExcluir))
                .ifPresent(activa -> {
                    activa.setActiva(false);
                    campanaRepository.saveAndFlush(activa);
                });
    }

    private Campana buscarOFallar(Long id) {
        return campanaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Campana", id));
    }

    private CampanaResponse toResponse(Campana campana) {
        return new CampanaResponse(
                campana.getId(),
                campana.getProducto().getId(),
                campana.getProducto().getNombre(),
                campana.getNombre(),
                campana.getAnio(),
                campana.getFechaInicio(),
                campana.getFechaFin(),
                campana.isActiva()
        );
    }
}
