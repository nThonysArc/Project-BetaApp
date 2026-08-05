package pe.betaagroindustrial.avance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import pe.betaagroindustrial.avance.producto.Producto;
import pe.betaagroindustrial.avance.producto.dto.ProductoRequest;
import pe.betaagroindustrial.avance.producto.dto.ProductoResponse;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductoMapper {

    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "id", ignore = true)
    void actualizarEntity(ProductoRequest request, @MappingTarget Producto producto);
}
