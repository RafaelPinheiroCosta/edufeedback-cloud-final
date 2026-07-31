# Infraestrutura Azure

Infraestrutura como código do EduFeedback, implementada com Bicep e implantada no escopo de um Resource Group.

## Recursos

- Storage Account;
- filas `feedback-critical-notifications` e poison gerenciada pelo runtime;
- Log Analytics Workspace;
- Application Insights;
- Key Vault;
- PostgreSQL Flexible Server e banco `edufeedbackdb`;
- Azure Communication Services;
- Email Communication Service e domínio gerenciado;
- plano Linux Consumption `Y1`;
- três Azure Function Apps Java 21;
- Managed Identity para cada Function App;
- RBAC de leitura de segredos no Key Vault.

## Organização

```text
infra/
├── main.bicep
├── bicepconfig.json
├── parameters/
│   ├── dev.bicepparam
│   └── prod.bicepparam
├── modules/
│   ├── communication.bicep
│   ├── functions.bicep
│   ├── keyvault.bicep
│   ├── monitoring.bicep
│   ├── postgres.bicep
│   └── storage.bicep
└── scripts/
    ├── configure-github-oidc.ps1
    └── deploy-prod.ps1
```

## Configuração das Function Apps

O módulo `functions.bicep` configura:

- Azure Functions runtime v4;
- Java 21 em Linux;
- HTTPS obrigatório;
- TLS mínimo 1.2;
- FTPS desabilitado;
- perfil Quarkus `prod`;
- Application Insights;
- acesso ao Storage;
- referências do Key Vault para banco e e-mail;
- nomes das filas e aplicações;
- timezone da aplicação.

## Segredos

O código não contém senha do PostgreSQL nem connection strings do banco e do serviço de e-mail. O deployment recebe os valores sensíveis externamente e cria ou referencia segredos no Key Vault.

Managed Identities das Function Apps recebem a função `Key Vault Secrets User` no cofre.

## Parâmetros de produção

O arquivo `parameters/prod.bicepparam` contém apenas valores não sensíveis. Os parâmetros obrigatórios externos são:

```text
databaseAdminPassword
adminEmail
```

## Deployment local

Pré-requisitos:

- Azure CLI autenticada;
- Bicep CLI;
- permissão para deployments no Resource Group;
- permissão para criar recursos e role assignments.

Execute no PowerShell:

```powershell
.\infra\scripts\deploy-prod.ps1 `
  -ResourceGroup rg-edufeedback-prod `
  -AdminEmail administrador@exemplo.com
```

O script:

1. solicita a senha do PostgreSQL como `SecureString`;
2. gera um arquivo de parâmetros temporário;
3. atualiza o Bicep CLI;
4. executa `validate`;
5. executa `what-if`;
6. solicita confirmação;
7. aplica o deployment;
8. remove o arquivo temporário e limpa a senha da memória gerenciada.

## Validação manual

```bash
az bicep build --file infra/main.bicep
```

```bash
az deployment group validate \
  --resource-group rg-edufeedback-prod \
  --parameters infra/parameters/prod.generated.bicepparam
```

```bash
az deployment group what-if \
  --resource-group rg-edufeedback-prod \
  --parameters infra/parameters/prod.generated.bicepparam
```

O arquivo `prod.generated.bicepparam` é temporário e não deve ser versionado.

## GitHub Actions

O workflow `.github/workflows/infra.yml` usa OIDC e requer:

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
ADMIN_EMAIL
POSTGRES_ADMIN_PASSWORD
```

Em Pull Request, o workflow compila, valida e executa `what-if`. Em push para `main` ou execução manual, também aplica a infraestrutura.

## Configuração OIDC

O script `configure-github-oidc.ps1` cria a credencial federada para o repositório informado. O principal usado pelo GitHub precisa das permissões adequadas no escopo do Resource Group.

## Ordem operacional

1. validar o Bicep;
2. revisar o `what-if`;
3. aplicar a infraestrutura;
4. confirmar segredos e referências do Key Vault;
5. executar o workflow de aplicação;
6. validar health, API, fila, e-mail, relatório e Application Insights.
