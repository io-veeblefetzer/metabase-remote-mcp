# Task 2.1: Maven Plugin Configuration

**Task ID:** 2.1  
**Phase:** 2 - OpenAPI Client Generation  
**GitHub Issue:** [#2](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/2)  
**Status:** ⬜ Not Started

---

## Objective

Configure the OpenAPI Generator Maven plugin to automatically generate a type-safe Java client from the Metabase API specification during the build process.

---

## Prerequisites

- Phase 1 completed (infrastructure setup)
- `src/main/resources/metabase-api-spec.json` exists
- Basic understanding of Maven build lifecycle

---

## Technical Details

### File to Modify

**File:** `pom.xml`

### Plugin Configuration

Add the following plugin configuration to the `<build><plugins>` section:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.2.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <!-- Input specification -->
                <inputSpec>${project.basedir}/src/main/resources/metabase-api-spec.json</inputSpec>
                
                <!-- Generator type -->
                <generatorName>java</generatorName>
                
                <!-- Output directory -->
                <output>${project.build.directory}/generated-sources/openapi</output>
                
                <!-- Package structure -->
                <apiPackage>io.veeblefetzer.metabase.api</apiPackage>
                <modelPackage>io.veeblefetzer.metabase.model</modelPackage>
                <invokerPackage>io.veeblefetzer.metabase.client</invokerPackage>
                
                <!-- Additional configuration -->
                <configOptions>
                    <library>webclient</library>
                    <dateLibrary>java8</dateLibrary>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useJakartaEe>true</useJakartaEe>
                    <generateApiTests>false</generateApiTests>
                    <generateModelTests>false</generateModelTests>
                    <generateApiDocumentation>false</generateApiDocumentation>
                    <generateModelDocumentation>false</generateModelDocumentation>
                    <serializableModel>true</serializableModel>
                </configOptions>
                
                <!-- Skip validation (Metabase spec may have minor issues) -->
                <skipValidateSpec>true</skipValidateSpec>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Required Dependencies

Add these dependencies for the generated client:

```xml
<!-- WebClient support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- OpenAPI/Jackson annotations -->
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>2.2.19</version>
</dependency>

<!-- Jackson for JSON processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>

<!-- Nullable annotations -->
<dependency>
    <groupId>com.google.code.findbugs</groupId>
    <artifactId>jsr305</artifactId>
    <version>3.0.2</version>
</dependency>
```

### Build Helper Plugin

Add this plugin to include generated sources:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>add-source</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>add-source</goal>
            </goals>
            <configuration>
                <sources>
                    <source>${project.build.directory}/generated-sources/openapi/src/main/java</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### .gitignore Update

Add to `.gitignore`:
```
# Generated sources
target/generated-sources/
```

---

## Implementation Checklist

- [ ] Review existing `pom.xml` structure
- [ ] Add `openapi-generator-maven-plugin` to plugins section
- [ ] Configure plugin with correct input spec path
- [ ] Set appropriate package names for generated code
- [ ] Configure WebClient library option
- [ ] Add `build-helper-maven-plugin` for source inclusion
- [ ] Add required dependencies (webflux, swagger, jackson)
- [ ] Verify `.gitignore` excludes generated sources
- [ ] Review `metabase-api-spec.json` for validity
- [ ] Test: Run `mvn clean generate-sources`
- [ ] Test: Verify generated code in `target/generated-sources/openapi/`
- [ ] Test: Run `mvn clean compile` successfully
- [ ] Test: Verify no compilation errors in generated code

---

## Acceptance Criteria

- [ ] `openapi-generator-maven-plugin` is properly configured in `pom.xml`
- [ ] All required dependencies are added
- [ ] `mvn generate-sources` executes without errors
- [ ] Generated Java client exists in `target/generated-sources/openapi/`
- [ ] Generated code follows the specified package structure:
  - API classes in `io.veeblefetzer.metabase.api`
  - Model classes in `io.veeblefetzer.metabase.model`
  - Client classes in `io.veeblefetzer.metabase.client`
- [ ] `mvn clean compile` completes successfully
- [ ] Generated sources are excluded from git

---

## Generated Package Structure

After generation, expect this structure:
```
target/generated-sources/openapi/src/main/java/
└── io/
    └── veeblefetzer/
        └── metabase/
            ├── api/
            │   ├── DatabaseApi.java
            │   ├── CardApi.java
            │   ├── DashboardApi.java
            │   ├── QueryApi.java
            │   └── ...
            ├── model/
            │   ├── Database.java
            │   ├── Card.java
            │   ├── Dashboard.java
            │   └── ...
            └── client/
                ├── ApiClient.java
                └── ...
```

---

## Verification Commands

```bash
# Generate sources
mvn clean generate-sources

# Check generated files exist
ls -la target/generated-sources/openapi/src/main/java/io/veeblefetzer/metabase/

# Compile entire project
mvn clean compile

# View specific generated API class
cat target/generated-sources/openapi/src/main/java/io/veeblefetzer/metabase/api/DatabaseApi.java | head -50
```

---

## Troubleshooting

### Spec Validation Errors

If the OpenAPI spec has validation issues:
```xml
<skipValidateSpec>true</skipValidateSpec>
```

### Missing Dependencies

If compilation fails with missing classes, check:
1. All dependencies are in `<dependencies>` section
2. Versions are compatible with Spring Boot 3.x
3. Jakarta EE packages are used (not javax)

### Generated Code Not in Classpath

Ensure `build-helper-maven-plugin` is configured correctly and runs in `generate-sources` phase.

---

## Commit Template

```
feat(client): configure OpenAPI Generator Maven plugin

- Add openapi-generator-maven-plugin with webclient library
- Configure package structure for generated code
- Add build-helper-maven-plugin for source inclusion
- Add required dependencies (webflux, swagger, jackson)
- Update .gitignore to exclude generated sources

Part of #2
```

---

## Notes

- Generated code should NOT be committed to git
- Regenerate after any changes to `metabase-api-spec.json`
- The WebClient library is chosen for reactive support
- Spring Boot 3.x requires Jakarta EE namespaces
- Some Metabase API endpoints may not have complete specs
