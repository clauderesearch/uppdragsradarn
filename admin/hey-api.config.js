// hey-api.config.js
export default {
  // Input OpenAPI spec file
  input: '../api-spec/openapi.json',
  
  // Output directory for generated client
  output: './api-client',
  
  // Generator to use (TypeScript + fetch)
  generator: 'typescript-fetch',
  
  // Additional configuration options
  config: {
    // Use ESM modules
    supportsES6: true,
    
    // Generate models as TypeScript interfaces
    modelPropertyNaming: 'camelCase',
    
    // Add configuration for authentication
    withInterfaces: true,
    
    // Add response types
    typescriptThreePlus: true,
    
    // Additional options
    npmName: '@api/admin-client',
    npmVersion: '1.0.0',
    withSeparateModelsAndApi: true,
    apiPackage: 'api',
    modelPackage: 'models'
  }
}