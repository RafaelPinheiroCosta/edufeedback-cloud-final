CREATE TABLE edufeedback.notificacao (
  id UUID PRIMARY KEY,
  avaliacao_id UUID NOT NULL REFERENCES edufeedback.avaliacao(id) ON DELETE RESTRICT,
  event_id UUID NOT NULL,
  tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('EMAIL_CRITICO','EMAIL_RELATORIO')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDENTE','ENVIANDO','ENVIADA','FALHOU')),
  destinatario VARCHAR(320) NOT NULL,
  mensagem TEXT NOT NULL,
  tentativas INTEGER NOT NULL DEFAULT 0 CHECK (tentativas >= 0),
  ultimo_erro TEXT,
  data_envio TIMESTAMPTZ,
  versao BIGINT NOT NULL DEFAULT 0,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_notificacao_evento_tipo UNIQUE(event_id, tipo)
);
CREATE INDEX idx_notificacao_avaliacao ON edufeedback.notificacao(avaliacao_id);
