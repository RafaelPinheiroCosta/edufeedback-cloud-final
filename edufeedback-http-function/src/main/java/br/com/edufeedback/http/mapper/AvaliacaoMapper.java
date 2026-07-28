package br.com.edufeedback.http.mapper;

import br.com.edufeedback.http.dto.AvaliacaoResponse;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AvaliacaoMapper {
  public AvaliacaoResponse toResponse(AvaliacaoEntity entity) {
    return new AvaliacaoResponse(
        entity.id,
        entity.descricao,
        entity.nota,
        entity.urgencia.name(),
        entity.dataEnvio,
        entity.correlationId);
  }
}
