# Claude Guidelines

## General Guidelines
- Make minimalist changes that don't break with previous conventions
- Respect existing architecture and coding patterns
- Use existing subdomain patterns (dev.uppdragsradarn.se)
- Avoid creating new subdomains unnecessarily
- Prefer small, targeted fixes over large refactors
- Follow clean code principles
- Use systematic task planning with todo lists for complex changes
- Remove deprecated code to avoid technical debt
- Always use nohup when launching npm run dev or spring boot run

## Architecture Patterns
- Follow hexagonal architecture with domain-driven design
- Maintain separation between domain, application, and infrastructure layers
- Use Controller-Service-Repository pattern for backend components
- Avoid creating interfaces (like LocationService) that have only a single implementation
- Only create service interfaces when there's a genuine need for multiple implementations
- Componentized frontend with reusable elements
- Provider pattern for crawler implementations
- DTO pattern for API data transfer
- Rest API endpoints with consistent naming

## Technology Stack
- Java 21 LTS
  - Use Java Records when possible for data transfer objects
  - Take advantage of modern Java features
- Spring Boot 3
- Nuxt 3 for frontend with Vue Composition API
- PostgreSQL with Liquibase migrations
  - Under development, simply drop/recreate database rather than writing complex migration scripts
  - Focus on maintainable schema design over migration complexity
- Redis for session management
- OAuth2/OIDC (Cognito) for authentication

## Naming Conventions
- Java classes: PascalCase
- Java methods/variables: camelCase
- Java constants: UPPER_SNAKE_CASE
- Interface names without "I" prefix
- Implementation classes without "Impl" suffix (use DDD)
- Vue components: PascalCase
- Composables: camelCase with "use" prefix

## Coding Style
- Java: Google Java Style (2-space indentation)
- Vue/JS: 2-space indentation
- Single-file Vue components with composition API
- Appropriate exception handling with custom exceptions
- Comprehensive logging (SLF4J)
- Security-first approach with proper authentication and authorization
- Use session-based CSRF protection
- Centralize security mechanisms in the appropriate layers
- All JPA entities must implement equals() and hashCode() based on their database key

## Entity equals() and hashCode() Implementation

All JPA entities should follow this pattern to implement database key-based equality:

```java
@Entity
@Table(name = "entity_name")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // Only include explicitly marked fields
@ToString(exclude = {"collection1", "collection2"}) // Exclude collections to prevent recursion
public class EntityName {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include  // Mark the ID field for inclusion in equals/hashCode
  private UUID id;
  
  // Other fields...
  
  // Collections that should be excluded from toString
  @OneToMany(mappedBy = "entityName")
  @Builder.Default
  private Set<RelatedEntity> collection1 = new HashSet<>();
}
```

### Key Points

1. Use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` to ensure only the ID field is used
2. Mark the ID field with `@EqualsAndHashCode.Include`
3. Use `@ToString(exclude = {...})` to prevent recursive toString() calls on bidirectional relationships
4. For composite keys, apply `@EqualsAndHashCode.Include` to all fields that make up the key

### Special Cases

#### Composite Keys with @IdClass

For entities with composite keys using @IdClass:

```java
@Entity
@IdClass(CompositeKeyClass.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntityWithCompositeKey {

  @Id
  @EqualsAndHashCode.Include
  private UUID firstKeyPart;
  
  @Id
  @EqualsAndHashCode.Include
  private UUID secondKeyPart;
}
```

#### Entities with @EmbeddedId

For entities with embedded IDs:

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntityWithEmbeddedId {

  @EmbeddedId
  @EqualsAndHashCode.Include
  private EmbeddedKeyClass id;
}
```

### Why This Matters

- Proper equals() and hashCode() are essential for collections (HashSet, HashMap)
- Prevents infinite recursion in bidirectional relationships
- Allows proper entity comparison based on identity rather than all fields
- Required for reliable operation with JPA's entity manager and cache

### Testing Equals and HashCode

Always test that your equals and hashCode implementation works properly:

1. An entity should be equal to itself
2. Two entities with the same ID should be equal
3. Two entities with different IDs should not be equal
4. Two new entities (null IDs) should not be equal (they're different entities)
5. hashCode should produce the same value for equal entities

## Testing
- Write comprehensive unit and integration tests
- Use mocks for external dependencies
- WireMock for external API simulation
- Separate test configurations

## Linting & Quality Checks
- Use npm run lint for frontend code
- Maven builds with Spotless, PMD, and SpotBugs for backend

## Environment Configuration
- In development, use dev.uppdragsradarn.se as the primary domain
- Configure service hosts using docker-compose and nginx rather than DNS
- Use environment variables for secrets and configuration
- Always source environment variables before running Spring Boot applications with: `source ./load-env.sh`
- Use nohup when starting services to prevent timeout: `nohup source ./load-env.sh && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &`

## Common Issues and Solutions
- Removed location field in Assignment model:
  - The query `findByTitleAndLocation` in AssignmentRepository needs to be updated when this field is removed
  - Remove any reference to `a.location` in JPQL queries since this field no longer exists
- OAuth2 configuration:
  - When starting application locally, ensure all required environment variables are set
  - For testing without OAuth2, use the test profile which disables OAuth2 auto-configuration

## Implementation Process
- For complex changes:
  1. Examine current implementation and understand the requirements
  2. Create a structured todo list for all required tasks
  3. Mark tasks as in-progress when working on them
  4. Complete one task at a time and mark them completed
  5. Verify changes after implementation
  6. Remove deprecated code after migration is complete
- For security implementations:
  - Prefer to consolidate security mechanisms
  - Ensure consistent implementation across both frontend apps
  - Add tests to verify functionality

## Test Environment Setup
- Test URL: https://dev.uppdragsradarn.se
- Test credentials:
  - Email: jahziah.wagner@gmail.com
  - Password: T4SBLx9:^'ai%Wq
- Backend startup procedure:
  1. Navigate to backend directory: `cd backend`
  2. Load environment variables: `source ./load-env.sh`
  3. Start backend with OAuth: `nohup ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &`
  4. The backend will run on port 8080 with full OAuth2/Cognito integration
- Environment file: `.env.dev` contains necessary Cognito and OpenAI configurations