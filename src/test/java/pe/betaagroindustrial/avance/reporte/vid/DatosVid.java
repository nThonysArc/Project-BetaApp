package pe.betaagroindustrial.avance.reporte.vid;

import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.HoraVid;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.ModuloHora;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Ayudas para armar datos de prueba del avance VID. */
final class DatosVid {

    static final LocalDate D27 = LocalDate.of(2025, 12, 27);
    static final LocalDate D28 = LocalDate.of(2025, 12, 28);

    private DatosVid() {
    }

    static ModuloHora modulo(long id, int personas, int jabas, String supervisor, String obs) {
        return new ModuloHora(id, "MODULO " + id, "Sweet Globe", personas, BigDecimal.valueOf(jabas), supervisor, obs, false);
    }

    static ModuloHora almuerzo(long id, int personas) {
        return new ModuloHora(id, "MODULO " + id, "Sweet Globe", personas, BigDecimal.ZERO, "ANA", null, true);
    }

    static HoraVid hora(int n, LocalTime ini, LocalTime fin, LocalDate dia, ModuloHora... modulos) {
        return new HoraVid(n, ini, fin, dia, null, List.of(modulos));
    }

    static LocalTime t(int h, int m) {
        return LocalTime.of(h, m);
    }
}
