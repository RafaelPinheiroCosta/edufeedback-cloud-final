package br.com.edufeedback.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculadoraUrgenciaTest {
  @Test
  void deveClassificarFaixas() {
    assertThat(CalculadoraUrgencia.classificar(0)).isEqualTo(Urgencia.CRITICA);
    assertThat(CalculadoraUrgencia.classificar(4)).isEqualTo(Urgencia.CRITICA);
    assertThat(CalculadoraUrgencia.classificar(5)).isEqualTo(Urgencia.ATENCAO);
    assertThat(CalculadoraUrgencia.classificar(7)).isEqualTo(Urgencia.ATENCAO);
    assertThat(CalculadoraUrgencia.classificar(8)).isEqualTo(Urgencia.NORMAL);
    assertThat(CalculadoraUrgencia.classificar(10)).isEqualTo(Urgencia.NORMAL);
  }

  @Test
  void deveRejeitarNotaInvalida() {
    assertThatThrownBy(() -> CalculadoraUrgencia.classificar(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CalculadoraUrgencia.classificar(11))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
