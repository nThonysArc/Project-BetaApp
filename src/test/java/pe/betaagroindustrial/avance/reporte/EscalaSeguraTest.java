package pe.betaagroindustrial.avance.reporte;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EscalaSeguraTest {

    @Test
    void reporteNormalConservaLaEscalaPedida() {
        // 886 x 1300 unidades a 3x = ~10 millones de pixeles: dentro del tope
        assertThat(ReporteExcelRenderer.escalaSegura(886, 1300, 3.0)).isEqualTo(3.0);
    }

    @Test
    void reporteMuyLargoBajaLaEscalaParaNoPasarElTopeDePixeles() {
        double e = ReporteExcelRenderer.escalaSegura(886, 3000, 3.0);
        assertThat(e).isLessThan(3.0).isGreaterThanOrEqualTo(1.5);
        assertThat(886 * e * 3000 * e).isLessThanOrEqualTo(16_000_000.0 * 1.001);
    }

    @Test
    void nuncaBajaDelMinimo() {
        assertThat(ReporteExcelRenderer.escalaSegura(886, 20000, 3.0)).isEqualTo(1.5);
    }
}
