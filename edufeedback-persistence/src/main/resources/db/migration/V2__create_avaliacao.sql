CREATE TABLE edufeedback.avaliacao (
  id UUID PRIMARY KEY,
  descricao TEXT NOT NULL,
  nota SMALLINT NOT NULL CHECK (nota BETWEEN 0 AND 10),
  urgencia VARCHAR(20) NOT NULL CHECK (urgencia IN ('CRITICA','ATENCAO','NORMAL')),
  data_envio TIMESTAMPTZ NOT NULL,
  correlation_id UUID NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (char_length(descricao) BETWEEN 10 AND 2000)
);
CREATE INDEX idx_avaliacao_data_urgencia ON edufeedback.avaliacao (data_envio, urgencia);
CREATE INDEX idx_avaliacao_correlation_id ON edufeedback.avaliacao (correlation_id);
