[CmdletBinding()]
param(
  [Parameter(Mandatory)][string]$GitHubOwner,
  [Parameter(Mandatory)][string]$GitHubRepository,
  [string]$AppId = 'e8a444a2-169e-433c-b2ab-588bf6623627',
  [string]$Branch = 'main'
)
$ErrorActionPreference = 'Stop'
$credential = @{
  name = "github-$GitHubOwner-$GitHubRepository-$Branch"
  issuer = 'https://token.actions.githubusercontent.com'
  subject = "repo:$GitHubOwner/$GitHubRepository`:ref:refs/heads/$Branch"
  description = 'GitHub Actions OIDC - EduFeedback produção'
  audiences = @('api://AzureADTokenExchange')
} | ConvertTo-Json
$temp = New-TemporaryFile
try {
  Set-Content -Path $temp -Value $credential -Encoding utf8
  az ad app federated-credential create --id $AppId --parameters $temp
} finally { Remove-Item $temp -Force -ErrorAction SilentlyContinue }
Write-Host 'OIDC configurado. Cadastre no GitHub Variables: AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_SUBSCRIPTION_ID e ADMIN_EMAIL.'
Write-Host 'Cadastre em GitHub Secrets apenas POSTGRES_ADMIN_PASSWORD.'
