package pe.betaagroindustrial.avance.usuario.dto;

import java.util.List;
import java.util.Map;

/** Lo que necesita la pantalla para dibujar la checklist: catalogo + plantilla inicial por rol. */
public record CatalogoPermisosResponse(
        List<PermisoItem> permisos,
        Map<String, List<String>> plantillas
) {
    public record PermisoItem(String codigo, String modulo, String accion, String descripcion, List<String> requiere) {
    }
}
