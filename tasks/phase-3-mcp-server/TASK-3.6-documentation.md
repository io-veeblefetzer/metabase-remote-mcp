# Task 3.6: MCP Documentation

**Task ID:** 3.6  
**Phase:** 3 - MCP Server Implementation  
**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)  
**Status:** ⬜ Not Started

---

## Objective

Create comprehensive documentation for the MCP server, including tool reference guides, developer documentation, and usage examples to enable both end-users and developers to effectively use and extend the server.

---

## Prerequisites

- Task 3.1 through 3.5 completed
- All features implemented and tested
- Understanding of target audiences (developers, AI users)

---

## Technical Details

### Documentation Files to Create

```
docs/
├── MCP_TOOLS.md           # Tool reference documentation
├── MCP_RESOURCES.md       # Resource reference documentation
├── DEVELOPMENT.md         # Developer guide
├── DEPLOYMENT.md          # Deployment instructions
└── examples/
    ├── python-client.md   # Python client example
    └── curl-examples.md   # cURL command examples
```

### Also Update

- `README.md` (project root) - Quick start and overview
- Javadoc comments in source code

---

## Documentation Content

### 1. MCP_TOOLS.md

```markdown
# Metabase MCP Tools Reference

This document provides detailed information about all available MCP tools
for interacting with Metabase.

## Overview

The Metabase MCP Server exposes 10 tools for database operations, 
querying, dashboards, and more.

## Tool Categories

### Database Operations
- [list-databases](#list-databases)
- [get-database](#get-database)

### Query Operations
- [execute-query](#execute-query)

### Dashboard Operations
- [list-dashboards](#list-dashboards)
- [get-dashboard](#get-dashboard)

### Card/Question Operations
- [list-cards](#list-cards)
- [get-card](#get-card)
- [create-card](#create-card)

### User Operations
- [get-current-user](#get-current-user)

### Collection Operations
- [list-collections](#list-collections)

---

## Tool Reference

### list-databases

List all databases connected to Metabase.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| include_tables | boolean | No | Include table information (default: false) |

**Example Request:**
```json
{
  "name": "list-databases",
  "arguments": {
    "include_tables": true
  }
}
```

**Example Response:**
```json
{
  "databases": [
    {
      "id": 1,
      "name": "Sample Database",
      "engine": "postgres",
      "is_sample": true
    }
  ]
}
```

---

### get-database

Get detailed information about a specific database.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| database_id | integer | Yes | The database ID |
| include_metadata | boolean | No | Include table/field metadata (default: false) |

**Example Request:**
```json
{
  "name": "get-database",
  "arguments": {
    "database_id": 1,
    "include_metadata": true
  }
}
```

---

### execute-query

Execute a native SQL query against a database.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| database_id | integer | Yes | The database to query |
| query | string | Yes | SQL query to execute |
| limit | integer | No | Max rows to return (default: 100) |

**Example Request:**
```json
{
  "name": "execute-query",
  "arguments": {
    "database_id": 1,
    "query": "SELECT * FROM customers WHERE country = 'USA'",
    "limit": 50
  }
}
```

**Example Response:**
```json
{
  "columns": ["id", "name", "email", "country"],
  "rows": [
    [1, "John Doe", "john@example.com", "USA"],
    [2, "Jane Smith", "jane@example.com", "USA"]
  ],
  "row_count": 2
}
```

**⚠️ Security Note:**
Queries are executed with the permissions of the Metabase API key.
Ensure proper access controls are in place.

---

[Continue with remaining tools...]
```

### 2. MCP_RESOURCES.md

```markdown
# Metabase MCP Resources Reference

This document describes the MCP resources available for reading
Metabase configuration and metadata.

## URI Scheme

All resources use the `metabase://` URI scheme.

## Available Resources

### Database Metadata
**URI:** `metabase://database/{id}`

Provides metadata about a Metabase database.

**Example:**
```
metabase://database/1
```

**Response:**
```json
{
  "id": 1,
  "name": "Sample Database",
  "engine": "postgres",
  "details": {...}
}
```

---

### Database Tables
**URI:** `metabase://database/{id}/tables`

Lists all tables with their schemas.

**Example:**
```
metabase://database/1/tables
```

---

### Dashboard Configuration
**URI:** `metabase://dashboard/{id}`

Full dashboard configuration including cards and layout.

---

### Card Definition
**URI:** `metabase://card/{id}`

Query and visualization settings for a saved question.

---

### Collection Contents
**URI:** `metabase://collection/{id}`

Lists items in a collection.

---

## Usage with MCP

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "resources/read",
  "params": {
    "uri": "metabase://database/1"
  }
}
```
```

### 3. DEVELOPMENT.md

```markdown
# Developer Guide

This guide helps developers understand, build, and extend the 
Metabase MCP Server.

## Table of Contents
1. [Project Structure](#project-structure)
2. [Building the Project](#building-the-project)
3. [Running Locally](#running-locally)
4. [Adding New Tools](#adding-new-tools)
5. [Adding New Resources](#adding-new-resources)
6. [Testing](#testing)
7. [Code Style](#code-style)

## Project Structure

```
src/main/java/io/veeblefetzer/remote_mcp_server/
├── RemoteMcpServerApplication.java    # Main application
├── config/
│   ├── MetabaseClientConfig.java      # API client configuration
│   └── MetabaseProperties.java        # Configuration properties
├── controller/
│   ├── McpSseController.java          # SSE endpoint
│   └── HealthController.java          # Health checks
├── mcp/
│   ├── McpProtocolHandler.java        # MCP protocol handling
│   └── model/                         # JSON-RPC models
├── tools/
│   ├── ToolRegistry.java              # Tool management
│   ├── ToolDefinition.java            # Tool interface
│   └── definitions/                   # Individual tools
├── resources/
│   ├── ResourceRegistry.java          # Resource management
│   └── definitions/                   # Individual resources
└── service/
    └── MetabaseToolExecutor.java      # Tool execution
```

## Building the Project

```bash
# Clone repository
git clone https://github.com/io-veeblefetzer/metabase-remote-mcp.git
cd metabase-remote-mcp

# Build with Maven
mvn clean package

# Skip tests during build
mvn clean package -DskipTests
```

## Running Locally

### Prerequisites
- Java 17+
- Docker and Docker Compose
- Maven 3.8+

### Start Development Environment

```bash
# Start Metabase and PostgreSQL
docker-compose up -d

# Wait for Metabase to initialize
docker-compose logs -f metabase

# Set API key (after Metabase setup)
export METABASE_API_KEY=your-api-key

# Run the application
mvn spring-boot:run
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| METABASE_URL | Metabase base URL | http://localhost:3000 |
| METABASE_API_KEY | API key for auth | - |
| SERVER_PORT | MCP server port | 8080 |

## Adding New Tools

### 1. Create Tool Definition

```java
package io.veeblefetzer.remote_mcp_server.tools.definitions;

import io.veeblefetzer.remote_mcp_server.tools.ToolDefinition;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MyNewTool implements ToolDefinition {
    
    @Override
    public String getName() {
        return "my-new-tool";
    }
    
    @Override
    public String getDescription() {
        return "Description for AI assistants";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "param1", Map.of(
                    "type", "string",
                    "description", "Parameter description"
                )
            ),
            "required", List.of("param1")
        );
    }
}
```

### 2. Add Execution Logic

In `MetabaseToolExecutor.java`:

```java
case "my-new-tool" -> executeMyNewTool(params);

private Mono<ToolResult> executeMyNewTool(Map<String, Object> params) {
    // Implementation
}
```

### 3. Add Tests

Create unit and integration tests for your new tool.

## Adding New Resources

### 1. Create Resource Definition

```java
@Component
public class MyResource implements ResourceDefinition {
    
    @Override
    public String getUriTemplate() {
        return "metabase://myresource/{id}";
    }
    
    // ... other methods
}
```

### 2. Add Resolution Logic

In `ResourceResolver.java`:

```java
case "metabase://myresource/{id}" -> 
    resolveMyResource(Integer.parseInt(params.get("id")));
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=ToolRegistryTest

# Integration tests (requires Metabase)
METABASE_API_KEY=xxx mvn test -Dtest="*IT"
```

## Code Style

- Use Java 17 features (records, pattern matching, etc.)
- Follow Spring Boot conventions
- Write clear Javadoc for public APIs
- Use meaningful variable and method names
- Keep methods small and focused
```

### 4. DEPLOYMENT.md

```markdown
# Deployment Guide

This guide covers deploying the Metabase MCP Server in various environments.

## Table of Contents
1. [Docker Deployment](#docker-deployment)
2. [Kubernetes Deployment](#kubernetes-deployment)
3. [Cloud Deployment](#cloud-deployment)
4. [Configuration](#configuration)
5. [Monitoring](#monitoring)

## Docker Deployment

### Build Docker Image

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/remote-mcp-server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build image
docker build -t metabase-mcp-server:latest .

# Run container
docker run -d \
  --name mcp-server \
  -p 8080:8080 \
  -e METABASE_URL=http://metabase:3000 \
  -e METABASE_API_KEY=your-key \
  metabase-mcp-server:latest
```

### Docker Compose (Production)

```yaml
version: '3.8'
services:
  mcp-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - METABASE_URL=http://metabase:3000
      - METABASE_API_KEY=${METABASE_API_KEY}
    depends_on:
      - metabase
    healthcheck:
      test: curl --fail http://localhost:8080/health || exit 1
      interval: 30s
      timeout: 10s
      retries: 3
```

## Kubernetes Deployment

### Deployment Manifest

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: metabase-mcp-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: mcp-server
  template:
    metadata:
      labels:
        app: mcp-server
    spec:
      containers:
        - name: mcp-server
          image: metabase-mcp-server:latest
          ports:
            - containerPort: 8080
          env:
            - name: METABASE_URL
              value: "http://metabase-service:3000"
            - name: METABASE_API_KEY
              valueFrom:
                secretKeyRef:
                  name: metabase-secrets
                  key: api-key
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
          readinessProbe:
            httpGet:
              path: /health
              port: 8080
```

## Configuration

### Production Settings

```properties
# application-prod.properties
server.port=8080

# Metabase connection
metabase.url=${METABASE_URL}
metabase.api-key=${METABASE_API_KEY}
metabase.connect-timeout=15s
metabase.read-timeout=60s
metabase.max-connections=20

# Logging
logging.level.root=INFO
logging.level.io.veeblefetzer=INFO

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
```

## Monitoring

### Health Endpoints

- `/health` - Basic health check
- `/actuator/health` - Detailed health (if enabled)
- `/actuator/metrics` - Application metrics

### Logging

Configure structured logging for production:

```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

### Metrics

Key metrics to monitor:
- Request latency
- Tool execution time
- Error rates
- SSE connection count
```

### 5. Update README.md

```markdown
# Metabase Remote MCP Server

An MCP (Model Context Protocol) server that provides AI assistants with 
access to Metabase databases, dashboards, and queries.

## Features

- 🔌 **MCP Protocol Support** - Full JSON-RPC 2.0 over SSE
- 📊 **Database Access** - Query databases, list tables, get metadata
- 📈 **Dashboard Management** - Read and create dashboards
- ❓ **Question Builder** - Create and execute saved questions
- 🔒 **Secure** - API key authentication with Metabase

## Quick Start

### Prerequisites

- Java 17+
- Docker and Docker Compose
- Metabase instance with API access

### Installation

```bash
# Clone the repository
git clone https://github.com/io-veeblefetzer/metabase-remote-mcp.git
cd metabase-remote-mcp

# Start development environment
docker-compose up -d

# Build the server
mvn clean package

# Run the server
METABASE_API_KEY=your-key mvn spring-boot:run
```

### MCP Client Configuration

Add to your MCP client configuration:

```json
{
  "mcpServers": {
    "metabase": {
      "url": "http://localhost:8080/mcp/sse",
      "transport": "sse"
    }
  }
}
```

## Available Tools

| Tool | Description |
|------|-------------|
| list-databases | List connected databases |
| get-database | Get database details |
| execute-query | Run SQL queries |
| list-dashboards | List dashboards |
| get-dashboard | Get dashboard details |
| list-cards | List saved questions |
| get-card | Get question details |
| create-card | Create new questions |
| get-current-user | Get user info |
| list-collections | List collections |

## Documentation

- [Tool Reference](docs/MCP_TOOLS.md)
- [Resource Reference](docs/MCP_RESOURCES.md)
- [Developer Guide](docs/DEVELOPMENT.md)
- [Deployment Guide](docs/DEPLOYMENT.md)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) for details.
```

---

## Implementation Checklist

### Documentation Files
- [ ] Create `docs/` directory
- [ ] Write `docs/MCP_TOOLS.md` with all 10 tools
- [ ] Write `docs/MCP_RESOURCES.md` with all 5 resources
- [ ] Write `docs/DEVELOPMENT.md` developer guide
- [ ] Write `docs/DEPLOYMENT.md` deployment guide
- [ ] Create `docs/examples/` directory
- [ ] Write `docs/examples/curl-examples.md`

### Code Documentation
- [ ] Add Javadoc to all public classes
- [ ] Add Javadoc to all public methods
- [ ] Document complex algorithms inline
- [ ] Add package-info.java for key packages

### Project Files
- [ ] Update root `README.md`
- [ ] Ensure `LICENSE` file is complete
- [ ] Update `pom.xml` with project description

### Review
- [ ] Spell check all documentation
- [ ] Verify all code examples work
- [ ] Test all curl commands
- [ ] Review for completeness
- [ ] Get peer review

---

## Acceptance Criteria

- [ ] All documentation files exist and are well-formatted
- [ ] Tool documentation covers all 10 tools with examples
- [ ] Resource documentation covers all 5 resources
- [ ] Developer guide enables new contributors
- [ ] Deployment guide covers Docker and Kubernetes
- [ ] README provides clear quick start
- [ ] All code examples are tested and working
- [ ] Javadoc is complete for public APIs

---

## Commit Template

```
docs: add comprehensive MCP server documentation

Documentation:
- docs/MCP_TOOLS.md: Complete tool reference with examples
- docs/MCP_RESOURCES.md: Resource URI documentation
- docs/DEVELOPMENT.md: Developer guide for contributors
- docs/DEPLOYMENT.md: Docker and Kubernetes deployment
- docs/examples/: Working code examples

Code Documentation:
- Added Javadoc to all public APIs
- Added inline comments for complex logic
- Created package-info.java for key packages

Project:
- Updated README.md with quick start guide
- Added feature overview and tool listing

Closes #3
```

---

## Notes

- Write for two audiences: AI assistants using tools, developers extending code
- Include practical examples that can be copy-pasted
- Keep documentation close to code (Javadoc) for API stability
- Update documentation when code changes
- Use consistent formatting across all documents
