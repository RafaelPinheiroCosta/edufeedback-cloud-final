package br.com.edufeedback.http.dto;

import jakarta.validation.constraints.*;

public record CriarAvaliacaoRequest(
    @NotBlank @Size(min = 10, max = 2000) String descricao,
    @NotNull @Min(0) @Max(10) Integer nota) {}
