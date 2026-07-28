param location string
param workspaceName string
param appInsightsName string
param tags object

resource workspace 'Microsoft.OperationalInsights/workspaces@2025-07-01' = {
  name: workspaceName
  location: location
  tags: tags
  properties: {
    features: { enableLogAccessUsingOnlyResourcePermissions: true }
    publicNetworkAccessForIngestion: 'Enabled'
    publicNetworkAccessForQuery: 'Enabled'
    retentionInDays: 30
    sku: { name: 'PerGB2018' }
  }
}

resource insights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  tags: tags
  properties: {
    Application_Type: 'web'
    IngestionMode: 'LogAnalytics'
    WorkspaceResourceId: workspace.id
    RetentionInDays: 90
    publicNetworkAccessForIngestion: 'Enabled'
    publicNetworkAccessForQuery: 'Enabled'
  }
}

output workspaceId string = workspace.id
output appInsightsId string = insights.id
output connectionString string = insights.properties.ConnectionString
