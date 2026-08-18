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

    @Transactional(readOnly = true)
    public List<CampanaResponse> listar() {
        return campanaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
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

        // Si se crea una campana activa nueva para el mismo producto, desactiva
        // la anterior primero (regla de negocio: 1 campana activa por producto).
        campanaRepository.findByProductoIdAndActivaTrue(producto.getId())
                .ifPresent(activa -> {
                    activa.setActiva(false);
                    campanaRepository.save(activa);
                });

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
