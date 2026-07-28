package br.com.edufeedback.notification.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@OpenAPIDefinition(
    info =
        @Info(
            title = "EduFeedback Notification Function",
            version = "1.0.0",
            description = "Processamento de notificações críticas",
            contact = @Contact(name = "Rafael Pinheiro Costa"),
            license = @License(name = "Projeto acadêmico FIAP")))
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {}
