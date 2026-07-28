param name string
param location string
param tenantId string
param tags object

resource vault 'Microsoft.KeyVault/vaults@2024-11-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    tenantId: tenantId
    sku: { family: 'A', name: 'standard' }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 90
    publicNetworkAccess: 'Enabled'
  }
}

output id string = vault.id
output name string = vault.name
output uri string = vault.properties.vaultUri
