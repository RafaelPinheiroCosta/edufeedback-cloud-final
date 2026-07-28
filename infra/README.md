# Infraestrutura Bicep — EduFeedback Cloud

A infraestrutura foi modularizada sem alterar os módulos Java existentes. O primeiro deployment adota os recursos que já existem quando nome, tipo e Resource Group coincidem, e cria PostgreSQL, Communication Services, domínio gerenciado de e-mail, plano e três Function Apps.

## Execução local segura

Abra PowerShell na raiz do projeto:

```powershell
./infra/scripts/deploy-prod.ps1 -AdminEmail seu-email@dominio.com
```

O script executa `validate`, `what-if` e só aplica após confirmação. A senha do PostgreSQL é solicitada sem eco e não é salva no repositório.

## OIDC do GitHub

```powershell
./infra/scripts/configure-github-oidc.ps1 -GitHubOwner SEU_USUARIO -GitHubRepository SEU_REPOSITORIO
```

Variáveis do repositório: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `ADMIN_EMAIL`.
Segredo do repositório: `POSTGRES_ADMIN_PASSWORD`.

## Ordem recomendada

1. Execute o deployment local e confira o `what-if`.
2. Configure OIDC.
3. Rode manualmente `Infraestrutura Azure`.
4. Rode `Build e Deploy das Functions`.
5. Teste HTTP → PostgreSQL → Queue → Notification e o relatório.
