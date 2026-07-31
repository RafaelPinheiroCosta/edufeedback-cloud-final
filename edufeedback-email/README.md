# edufeedback-email

Adapter compartilhado para envio de e-mails por Azure Communication Services Email.

## Responsabilidades

- Expor o contrato `EmailSender`.
- Criar o cliente do Azure Communication Services.
- Enviar conteúdo HTML.
- Validar remetente, destinatário e configuração.
- Aguardar a conclusão da operação do provedor.
- Traduzir falhas para `EmailDeliveryException`.
- Registrar duração e resultado sem expor o conteúdo sensível da configuração.

## Componentes

| Tipo | Finalidade |
|---|---|
| `EmailSender` | Porta usada pelos serviços de Notification e Report |
| `AzureCommunicationEmailSender` | Implementação com o SDK da Azure |
| `EmailDeliveryException` | Falha semântica de entrega |

## Configuração

```text
app.email.connection-string
app.email.sender
```

Nos módulos executáveis:

```text
AZURE_COMMUNICATION_CONNECTION_STRING
EMAIL_SENDER
```

A configuração é opcional no bootstrap para permitir inicialização e health diagnostics. Uma tentativa real de envio sem os valores necessários falha de forma explícita.

## Fluxo

1. valida destinatário, assunto e corpo;
2. cria ou reutiliza o cliente do SDK;
3. inicia o envio assíncrono no provedor;
4. aguarda o status final;
5. retorna sucesso ou lança `EmailDeliveryException`.

## Build

```bash
./mvnw -pl edufeedback-email -am test
```

No Windows PowerShell:

```powershell
.\mvnw.cmd -pl edufeedback-email -am test
```

## Segurança

Connection strings devem permanecer em variáveis protegidas ou no Key Vault. Logs mascaram o destinatário e não registram a connection string nem o corpo completo do e-mail.
