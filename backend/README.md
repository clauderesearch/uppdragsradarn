# Uppdragsradarn Backend Service

This is the backend service for Uppdragsradarn, a consulting assignment aggregator application.

## Crawler Package

The crawler package uses a clean, modular architecture for fetching assignments from different sources:

### Design

1. **Strategy Pattern**:
   - `FetchStrategy`: Different strategies for fetching data (HTTP, Playwright)
   - `ParseStrategy`: Source-specific parsing logic

2. **Composition over Inheritance**:
   - `SimpleCrawler`: Combines fetch and parse strategies
   - No deep inheritance hierarchies

3. **Configuration**:
   - `CrawlerConfiguration`: Declarative configuration
   - `CrawlerFactory`: Creates crawlers based on source and configuration
   - `CrawlerRegistry`: Central registry for finding crawlers

### Adding a New Source

To add a new source:

1. Create a new `ParseStrategy` implementation for the source
2. Choose an appropriate `FetchStrategy` or create a new one if needed
3. Add a configuration in `CrawlerConfig`

### Legacy Code

Old crawler implementations have been moved to the `legacy` package and will be removed in future releases.

## Code Quality Tools

The project uses several code quality tools to ensure consistent style and detect potential issues:

### Spotless with Google Java Format

Format code according to Google Java Style:

```bash
# Check code formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

### PMD for Static Code Analysis

Run PMD to find potential code issues:

```bash
mvn pmd:check
mvn pmd:cpd-check  # Copy-Paste detection
```

### SpotBugs for Bug Detection

Run SpotBugs to detect potential bugs:

```bash
mvn spotbugs:check
```

### SonarQube Scanner

For more comprehensive analysis, use the SonarQube scanner:

```bash
# Requires SonarQube server or SonarCloud credentials
mvn sonar:sonar
```

For offline analysis, the SonarLint IDE plugin is recommended.