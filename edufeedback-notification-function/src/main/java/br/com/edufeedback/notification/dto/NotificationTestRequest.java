package br.com.edufeedback.notification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record NotificationTestRequest(@NotNull UUID feedbackId) {}
