package pe.betaagroindustrial.avance.usuario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Catalogo de permisos del sistema (modulo + accion). Vive en codigo porque cada permiso esta atado
 * a un endpoint o regla concreta; en la base solo se guarda QUE permisos tiene cada usuario
 * (tabla usuario_permiso).
 *
 * Los permisos se exponen a Spring Security como autoridades "PERM_<NOMBRE>".
 * "requiere" son dependencias funcionales: por ejemplo, para crear cortes hay que poder ver el
 * proceso y la materia prima; al guardar un usuario se agregan automaticamente.
 */
public enum Permiso {

    PROCESOS_VER("Procesos diarios", "Ver", "Ver los procesos diarios de la campana"),
    PROCESOS_CREAR("Procesos diarios", "Crear", "Abrir un nuevo proceso diario", "PROCESOS_VER"),
    PROCESOS_CERRAR("Procesos diarios", "Cerrar", "Cerrar un proceso diario", "PROCESOS_VER"),
    PROCESOS_REABRIR("Procesos diarios", "Reabrir", "Reabrir un proceso diario cerrado", "PROCESOS_VER"),

    MATERIA_PRIMA_VER("Materia prima", "Ver", "Ver el ingreso de materia prima (TOTAL A PROCESAR)", "PROCESOS_VER"),
    MATERIA_PRIMA_EDITAR("Materia prima", "Editar", "Registrar o corregir el ingreso de materia prima",
            "MATERIA_PRIMA_VER"),

    CORTES_VER("Cortes por hora", "Ver", "Ver los cortes de cada hora", "PROCESOS_VER"),
    CORTES_CREAR("Cortes por hora", "Crear", "Registrar el corte de una nueva hora", "CORTES_VER", "MATERIA_PRIMA_VER"),
    CORTES_EDITAR("Cortes por hora", "Editar", "Modificar un corte ya registrado", "CORTES_VER", "MATERIA_PRIMA_VER"),
    CORTES_EDITAR_CERRADO("Cortes por hora", "Editar con proceso cerrado",
            "Modificar cortes de un proceso ya cerrado (queda auditado)", "CORTES_EDITAR"),
    CORTES_AJUSTAR_TOTALES("Cortes por hora", "Ajustar totales",
            "Reemplazar manualmente los totales calculados de un corte", "CORTES_EDITAR"),

    REPORTE_VER("Reporte", "Ver y descargar imagen", "Generar la imagen del reporte de avance",
            "PROCESOS_VER", "CORTES_VER"),

    CATALOGOS_ADMINISTRAR("Catalogos", "Administrar",
            "Crear y editar productos, variedades, campanas, maquinas y katos"),
    SUPERVISORES_ADMINISTRAR("Supervisores", "Administrar", "Crear, editar y desactivar supervisores");

    private final String modulo;
    private final String accion;
    private final String descripcion;
    private final String[] requiere;

    Permiso(String modulo, String accion, String descripcion, String... requiere) {
        this.modulo = modulo;
        this.accion = accion;
        this.descripcion = descripcion;
        this.requiere = requiere;
    }

    public String modulo() {
        return modulo;
    }

    public String accion() {
        return accion;
    }

    public String descripcion() {
        return descripcion;
    }

    public List<Permiso> requiere() {
        return Arrays.stream(requiere).map(Permiso::valueOf).toList();
    }

    /** Nombre de la autoridad en Spring Security: "PERM_CORTES_CREAR". */
    public String authority() {
        return "PERM_" + name();
    }

    /** Agrega, de forma transitiva, los permisos de los que dependen los indicados. */
    public static Set<Permiso> expandir(Set<Permiso> permisos) {
        Set<Permiso> resultado = permisos == null || permisos.isEmpty()
                ? EnumSet.noneOf(Permiso.class)
                : EnumSet.copyOf(permisos);
        List<Permiso> pendientes = new ArrayList<>(resultado);
        while (!pendientes.isEmpty()) {
            Permiso actual = pendientes.remove(pendientes.size() - 1);
            for (Permiso dep : actual.requiere()) {
                if (resultado.add(dep)) {
                    pendientes.add(dep);
                }
            }
        }
        return resultado;
    }
}
