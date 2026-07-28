package br.com.edufeedback.http.dto;

import java.time.Instant;
import java.util.UUID;

public record AvaliacaoResponse(
    UUID id,
    String descricao,
    short nota,
    String urgencia,
    Instant dataEnvio,
    UUID correlationId) {}
