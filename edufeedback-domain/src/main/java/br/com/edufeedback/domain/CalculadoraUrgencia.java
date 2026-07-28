package br.com.edufeedback.domain;

public final class CalculadoraUrgencia {
  private CalculadoraUrgencia() {}

  public static Urgencia classificar(int nota) {
    if (nota < 0 || nota > 10) {
      throw new IllegalArgumentException("A nota deve estar entre 0 e 10.");
    }
    if (nota <= 4) return Urgencia.CRITICA;
    if (nota <= 7) return Urgencia.ATENCAO;
    return Urgencia.NORMAL;
  }
}
