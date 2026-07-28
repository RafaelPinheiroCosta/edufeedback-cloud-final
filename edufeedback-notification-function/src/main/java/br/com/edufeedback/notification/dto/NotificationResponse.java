package br.com.edufeedback.notification.dto;

import java.util.UUID;

public record NotificationResponse(String status, UUID eventId) {}
