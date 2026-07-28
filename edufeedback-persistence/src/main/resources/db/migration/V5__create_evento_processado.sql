CREATE TABLE edufeedback.evento_processado (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL,
  consumer VARCHAR(100) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSADO')),
  processed_at TIMESTAMPTZ NOT NULL,
  correlation_id UUID NOT NULL,
  CONSTRAINT uk_evento_consumer UNIQUE(event_id, consumer)
);
