[CmdletBinding()]
param(
  [string]$ResourceGroup = 'rg-edufeedback-prod',

  [Parameter(Mandatory = $false)]
  [string]$AdminEmail
)

$ErrorActionPreference = 'Stop'

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$infraDirectory = Split-Path -Parent $scriptDirectory
$parametersDirectory = Join-Path $infraDirectory 'parameters'

$sourceParametersFile = Join-Path $parametersDirectory 'prod.bicepparam'
$temporaryParametersFile = Join-Path `
    $parametersDirectory `
    "prod.generated.$([Guid]::NewGuid().ToString('N')).bicepparam"

$password = $null
$securePassword = $null
$bstrPointer = [IntPtr]::Zero

function Assert-LastCommandSucceeded {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Step
  )

  if ($LASTEXITCODE -ne 0) {
    throw "Falha na etapa '$Step'. Código de saída: $LASTEXITCODE."
  }
}

function ConvertTo-BicepStringLiteral {
  param(
    [AllowEmptyString()]
    [string]$Value
  )

  return $Value.Replace("'", "''")
}

try {
  if (-not (Test-Path -LiteralPath $sourceParametersFile)) {
    throw "Arquivo de parâmetros não encontrado: $sourceParametersFile"
  }

  if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
    $AdminEmail = Read-Host 'E-mail que receberá notificações e relatórios'
  }

  if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
    throw 'O e-mail do administrador é obrigatório.'
  }

  $securePassword = Read-Host `
        'Senha forte do administrador PostgreSQL' `
        -AsSecureString

  $bstrPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR(
          $securePassword
  )

  $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
          $bstrPointer
  )

  if ([string]::IsNullOrWhiteSpace($password)) {
    throw 'A senha do PostgreSQL não pode estar vazia.'
  }

  $escapedAdminEmail = ConvertTo-BicepStringLiteral -Value $AdminEmail
  $escapedPassword = ConvertTo-BicepStringLiteral -Value $password

  $parameterContent = Get-Content `
        -LiteralPath $sourceParametersFile `
        -Raw

  $generatedContent = @"
$parameterContent

param adminEmail = '$escapedAdminEmail'
param databaseAdminPassword = '$escapedPassword'
"@

  Set-Content `
        -LiteralPath $temporaryParametersFile `
        -Value $generatedContent `
        -Encoding utf8

  Write-Host ''
  Write-Host 'Atualizando o Bicep CLI...' -ForegroundColor Cyan

  az bicep upgrade
  Assert-LastCommandSucceeded -Step 'Atualização do Bicep CLI'

  Write-Host ''
  Write-Host 'Validando a infraestrutura...' -ForegroundColor Cyan

  az deployment group validate `
        --resource-group $ResourceGroup `
        --parameters $temporaryParametersFile

  Assert-LastCommandSucceeded -Step 'Validação da infraestrutura'

  Write-Host ''
  Write-Host 'Executando a análise what-if...' -ForegroundColor Cyan

  az deployment group what-if `
        --resource-group $ResourceGroup `
        --parameters $temporaryParametersFile

  Assert-LastCommandSucceeded -Step 'Análise what-if'

  Write-Host ''
  $confirmation = Read-Host 'Aplicar o deployment? (S/N)'

  if ($confirmation -notin @('S', 's')) {
    Write-Host 'Deployment cancelado pelo usuário.' -ForegroundColor Yellow
    return
  }

  $deploymentName = "edufeedback-$((Get-Date).ToString('yyyyMMdd-HHmmss'))"

  Write-Host ''
  Write-Host "Iniciando o deployment '$deploymentName'..." `
        -ForegroundColor Cyan

  az deployment group create `
        --name $deploymentName `
        --resource-group $ResourceGroup `
        --parameters $temporaryParametersFile

  Assert-LastCommandSucceeded -Step 'Deployment da infraestrutura'

  Write-Host ''
  Write-Host 'Deployment concluído com sucesso.' -ForegroundColor Green
}
catch {
  Write-Host ''
  Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}
finally {
  if (
  $bstrPointer -ne [IntPtr]::Zero
  ) {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstrPointer)
  }

  $password = $null
  $securePassword = $null

  if (Test-Path -LiteralPath $temporaryParametersFile) {
    Remove-Item `
            -LiteralPath $temporaryParametersFile `
            -Force `
            -ErrorAction SilentlyContinue
  }
}
