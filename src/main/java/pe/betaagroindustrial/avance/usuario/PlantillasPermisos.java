package pe.betaagroindustrial.avance.usuario;

import java.util.EnumSet;
import java.util.Set;

import static pe.betaagroindustrial.avance.usuario.Permiso.*;

/**
 * Permisos con los que se PRECARGA la checklist al crear un usuario de cada rol. Son solo un punto
 * de partida: el super admin puede marcar o desmarcar lo que quiera antes de guardar.
 */
public final class PlantillasPermisos {

    private PlantillasPermisos() {
    }

    public static Set<Permiso> de(Rol rol) {
        return switch (rol) {
            case SUPER_ADMIN, ADMIN -> EnumSet.allOf(Permiso.class);
            case SUPERVISOR -> Permiso.expandir(EnumSet.of(
                    PROCESOS_CREAR, PROCESOS_CERRAR, MATERIA_PRIMA_VER, CORTES_CREAR, CORTES_EDITAR, REPORTE_VER));
            case OPERADOR -> Permiso.expandir(EnumSet.of(
                    PROCESOS_VER, MATERIA_PRIMA_VER, CORTES_CREAR, REPORTE_VER));
        };
    }
}
