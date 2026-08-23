package pe.betaagroindustrial.avance.variedad;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.producto.Producto;
import pe.betaagroindustrial.avance.producto.ProductoRepository;
import pe.betaagroindustrial.avance.variedad.dto.VariedadRequest;
import pe.betaagroindustrial.avance.variedad.dto.VariedadResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VariedadService {

    private final VariedadRepository variedadRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<VariedadResponse> listarPorProducto(Long productoId, boolean incluirInactivas) {
        List<Variedad> variedades = incluirInactivas
                ? variedadRepository.findByProductoIdOrderByOrdenAsc(productoId)
                : variedadRepository.findByProductoIdAndActivoTrueOrderByOrdenAsc(productoId);
        return variedades.stream().map(this::toResponse).toList();
    }

    @Transactional
    public VariedadResponse crear(VariedadRequest request) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.productoId()));

        Variedad variedad = Variedad.builder()
                .producto(producto)
                .nombre(request.nombre())
                .orden(request.orden() != null ? request.orden() : 0)
                .activo(true)
                .build();

        return toResponse(variedadRepository.save(variedad));
    }

    @Transactional
    public VariedadResponse actualizar(Long id, VariedadRequest request) {
        Variedad variedad = buscarOFallar(id);
        variedad.setNombre(request.nombre());
        variedad.setOrden(request.orden() != null ? request.orden() : variedad.getOrden());
        return toResponse(variedadRepository.save(variedad));
    }

    @Transactional
    public void desactivar(Long id) {
        Variedad variedad = buscarOFallar(id);
        variedad.setActivo(false);
        variedadRepository.save(variedad);
    }

    @Transactional
    public VariedadResponse reactivar(Long id) {
        Variedad variedad = buscarOFallar(id);
        variedad.setActivo(true);
        return toResponse(variedadRepository.save(variedad));
    }

    private Variedad buscarOFallar(Long id) {
        return variedadRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Variedad", id));
    }

    private VariedadResponse toResponse(Variedad variedad) {
        return new VariedadResponse(
                variedad.getId(),
                variedad.getProducto().getId(),
                variedad.getNombre(),
                variedad.getOrden(),
                variedad.isActivo()
        );
    }
}
