// Lab 7. One storage account, so what-if is easy to read.
// Deploy with incremental mode only. Complete mode would delete the rest of rg-hrv-dev.
param location string = resourceGroup().location

resource sa 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: 'sthrvlab${uniqueString(resourceGroup().id)}'
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
}
