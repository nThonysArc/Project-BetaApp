package pe.betaagroindustrial.avance.reporte;

import org.junit.jupiter.api.Test;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FusionPorKatoTest {

    private static FilaReporte fila(long maq, String kato, String variedad, int jabas, String peso, int emp) {
        return new FilaReporte(maq, "MAQUINA " + maq, kato, variedad, BigDecimal.valueOf(jabas),
                new BigDecimal(peso), emp, BigDecimal.ZERO);
    }

    @Test
    void unaKatoConDosVariedadesSeMuestraEnUnaSolaFila() {
        List<FilaReporte> r = ReporteImagenService.fusionarPorKato(List.of(
                fila(3, "KATO 1", "VENTURA", 300, "1500.0", 6),
                fila(3, "KATO 1", "SEKOYA", 300, "1500.0", 6),
                fila(3, "KATO 2", "VENTURA", 600, "3000.0", 6)));

        assertThat(r).hasSize(2);
        assertThat(r.get(0).kato()).isEqualTo("KATO 1");
        assertThat(r.get(0).variedad()).isEqualTo("VENTURA/SEKOYA");
        assertThat(r.get(0).jabas()).isEqualByComparingTo("600");
        assertThat(r.get(0).peso()).isEqualByComparingTo("3000.0");
        assertThat(r.get(0).empacadores()).isEqualTo(6);
        assertThat(r.get(0).kgPorEmpacador()).isEqualByComparingTo("500.00");
        assertThat(r.get(1).variedad()).isEqualTo("VENTURA");
    }

    @Test
    void mismaKatoEnOtraMaquinaNoSeMezcla() {
        List<FilaReporte> r = ReporteImagenService.fusionarPorKato(List.of(
                fila(1, "KATO 1", "VENTURA", 100, "500.0", 4),
                fila(2, "KATO 1", "SEKOYA", 100, "500.0", 4)));
        assertThat(r).hasSize(2);
    }

    @Test
    void variedadRepetidaNoSeDuplicaEnElTexto() {
        List<FilaReporte> r = ReporteImagenService.fusionarPorKato(List.of(
                fila(1, "KATO 1", "VENTURA", 100, "500.0", 4),
                fila(1, "KATO 1", "VENTURA", 50, "250.0", 4)));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).variedad()).isEqualTo("VENTURA");
        assertThat(r.get(0).jabas()).isEqualByComparingTo("150");
    }

    @Test
    void empacadoresDeUnaKatoEsUnaSolaCantidadNoElMayor() {
        List<FilaReporte> r = ReporteImagenService.fusionarPorKato(List.of(
                fila(1, "KATO 1", "VENTURA", 100, "500.0", 5),
                fila(1, "KATO 1", "SEKOYA", 100, "500.0", 8)));
        assertThat(r).hasSize(1);
        assertThat(r.get(0).empacadores()).isEqualTo(5);
        assertThat(r.get(0).kgPorEmpacador()).isEqualByComparingTo("200.00");
    }
}
