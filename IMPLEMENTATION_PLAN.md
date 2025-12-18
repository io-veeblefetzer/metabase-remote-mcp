# Metabase Remote MCP Server - Implementation Plan

## Project Overview

**Project Name:** Metabase Remote MCP Server  
**Purpose:** Create an MCP (Model Context Protocol) server that acts as a remote gateway to Metabase resources, enabling AI assistants to execute queries and build dashboards  
**Technology Stack:** Java, Spring Boot, Spring AI, OpenAPI Generator, Docker, Server-Sent Events (SSE)  
**Version Control:** Git with Conventional Commits

---

## Development Methodology

### Workflow Process
1. **Plan** - Review and understand requirements for the current phase
2. **Implement** - Develop code following best practices
3. **Test** - Write and execute tests to verify functionality
4. **Compile** - Ensure project builds successfully
5. **Commit** - Create conventional commit with clear message
6. **Move to Next Phase** - Proceed to next implementation step

### Quality Standards
- Clean, maintainable code
- Comprehensive test coverage for all features
- Successful compilation at each checkpoint
- Adherence to Java and Spring Boot best practices
- Conventional commit messages for all changes

---

## Phase 1: Infrastructure Setup

**GitHub Issue:** [#1](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/1)

### Objective
Create a local development environment with Docker Compose orchestrating a Metabase instance and a database with sample data for testing.

### Technical Requirements

#### 1.1 Docker Compose Configuration
- **File:** `docker-compose.yml`
- **Services Required:**
  - **Metabase Service**
    - Image: `metabase/metabase:latest`
    - Port mapping: 3000:3000
    - Environment variables for configuration
    - Health checks
    - Dependencies on database service
  
  - **Database Service** (PostgreSQL recommended)
    - Image: `postgres:15-alpine`
    - Port mapping: 5432:5432
    - Volume for data persistence
    - Initialization scripts for sample data
    - Environment variables (credentials, database name)

#### 1.2 Sample Data Population
- **Location:** `docker/init-db/`
- **Files:**
  - `01-schema.sql` - Create database schema
  - `02-sample-data.sql` - Insert sample business data
  
- **Sample Data Requirements:**
  - Multiple tables representing a realistic business scenario
  - Sufficient data volume for meaningful queries (100+ rows per table)
  - Examples: customers, orders, products, transactions
  - Various data types (strings, numbers, dates, booleans)
  - Relationships between tables (foreign keys)

#### 1.3 Documentation
- **File:** `docker/README.md`
- **Contents:**
  - Quick start instructions
  - Environment variables explanation
  - Port mappings
  - Default credentials
  - Troubleshooting common issues

### Implementation Tasks

- [ ] Create `docker-compose.yml` with Metabase and PostgreSQL services
- [ ] Configure service dependencies and health checks
- [ ] Create `docker/init-db/` directory structure
- [ ] Write database schema SQL script
- [ ] Generate sample business data SQL script
- [ ] Add volume mappings for database persistence
- [ ] Document setup and usage in `docker/README.md`
- [ ] Test: Start services with `docker-compose up`
- [ ] Test: Verify Metabase accessible at http://localhost:3000
- [ ] Test: Verify database connection and sample data
- [ ] Test: Complete Metabase initial setup wizard
- [ ] Test: Create at least one sample dashboard/question in Metabase

### Acceptance Criteria

✓ `docker-compose.yml` exists and is properly configured  
✓ All services start successfully with `docker-compose up`  
✓ Metabase web interface is accessible at http://localhost:3000  
✓ PostgreSQL database is populated with sample data  
✓ Metabase can connect to the database  
✓ Documentation is clear and complete  
✓ Project compiles successfully  
✓ Conventional commit created: `feat: add Docker Compose infrastructure for Metabase and PostgreSQL`

---

## Phase 2: OpenAPI Client Generation

**GitHub Issue:** [#2](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/2)

### Objective
Configure OpenAPI Generator Maven plugin to automatically generate a type-safe Java client from the Metabase API specification, enabling programmatic interaction with Metabase.

### Technical Requirements

#### 2.1 Maven Plugin Configuration
- **File:** `pom.xml`
- **Plugin:** `openapi-generator-maven-plugin`
- **Configuration Parameters:**
  - Input spec: `src/main/resources/metabase-api-spec.json`
  - Generator: `java`
  - Library: `webclient` (Spring WebClient for reactive support)
  - Output directory: `target/generated-sources/openapi`
  - API package: `io.veeblefetzer.metabase.api`
  - Model package: `io.veeblefetzer.metabase.model`
  - Invoker package: `io.veeblefetzer.metabase.client`
  - Additional properties:
    - `dateLibrary=java8`
    - `useSpringBoot3=true`
    - `generateApiTests=false`
    - `generateModelTests=false`

#### 2.2 OpenAPI Specification Review
- **File:** `src/main/resources/metabase-api-spec.json`
- **Validation:**
  - Verify specification is valid OpenAPI 3.0+ format
  - Check completeness of endpoint definitions
  - Review authentication requirements
  - Identify key endpoints for MCP server implementation

#### 2.3 Build Configuration
- Configure Maven to generate sources during `generate-sources` phase
- Ensure generated code is included in compilation classpath
- Add generated sources to IDE source paths
- Configure `.gitignore` to exclude generated code

#### 2.4 Client Configuration Class
- **File:** `src/main/java/io/veeblefetzer/remote_mcp_server/config/MetabaseClientConfig.java`
- **Purpose:** Configure the generated API client with base URL and authentication
- **Configuration Properties:**
  - `metabase.url` - Base URL of Metabase instance
  - `metabase.api.key` - API key for authentication
  - `metabase.timeout` - Request timeout settings

### Implementation Tasks

- [ ] Review `src/main/resources/metabase-api-spec.json`
- [ ] Configure `openapi-generator-maven-plugin` in `pom.xml`
- [ ] Set up proper package structure for generated code
- [ ] Configure plugin execution phase
- [ ] Update `.gitignore` to exclude `target/generated-sources/`
- [ ] Run `mvn clean generate-sources`
- [ ] Verify generated API client classes exist
- [ ] Create `MetabaseClientConfig.java` configuration class
- [ ] Add configuration properties to `application.properties`
- [ ] Create simple integration test to verify client functionality
- [ ] Test: Compile project with `mvn clean compile`
- [ ] Test: Verify no compilation errors
- [ ] Test: Instantiate a generated API class
- [ ] Test: Make a test API call to Metabase (GET /api/user/current)

### Acceptance Criteria

✓ `openapi-generator-maven-plugin` configured in `pom.xml`  
✓ `mvn generate-sources` executes successfully  
✓ Generated Java client code exists in `target/generated-sources/openapi/`  
✓ Project compiles without errors: `mvn clean compile`  
✓ Generated API classes are accessible from project code  
✓ Configuration class created for API client setup  
✓ At least one integration test verifies client functionality  
✓ All tests pass: `mvn test`  
✓ Conventional commit created: `feat: configure OpenAPI Generator for Metabase API client`

---

## Phase 3: MCP Server Implementation with SSE

**GitHub Issue:** [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)

### Objective
Implement a Spring Boot-based MCP server that exposes Metabase API endpoints as MCP tools via Server-Sent Events (SSE), enabling AI assistants to interact with Metabase remotely.

### Technical Requirements

#### 3.1 MCP Server Architecture
- **Technology:** Spring AI MCP Server with SSE transport
- **Protocol:** Model Context Protocol (MCP) over Server-Sent Events
- **Endpoints:**
  - SSE endpoint: `/sse` - For MCP communication
  - Health endpoint: `/health` - For service monitoring

#### 3.2 Core Components

##### 3.2.1 MCP Server Controller
- **File:** `src/main/java/io/veeblefetzer/remote_mcp_server/controller/McpServerController.java`
- **Responsibilities:**
  - Handle SSE connections
  - Process MCP protocol messages
  - Route tool calls to appropriate handlers
  - Return responses in MCP format

##### 3.2.2 MCP Tool Definitions
- **Package:** `io.veeblefetzer.remote_mcp_server.tools`
- **Purpose:** Define MCP tools for each Metabase API operation
- **Tool Structure:**
  - Tool name (kebab-case naming)
  - Description
  - Input schema (JSON Schema)
  - Output format
  
**Key Tool Categories:**
1. **Database Tools** - List databases, get database metadata
2. **Query Tools** - Execute queries, get query results
3. **Dashboard Tools** - List dashboards, get dashboard details
4. **Card/Question Tools** - Create, read, update questions
5. **User Tools** - Get current user info
6. **Collection Tools** - Manage collections

##### 3.2.3 Tool Executor Service
- **File:** `src/main/java/io/veeblefetzer/remote_mcp_server/service/MetabaseToolExecutor.java`
- **Responsibilities:**
  - Map MCP tool calls to Metabase API client methods
  - Handle authentication
  - Process request parameters
  - Transform responses to MCP format
  - Error handling and logging

##### 3.2.4 Resource Definitions
- **Package:** `io.veeblefetzer.remote_mcp_server.resources`
- **Purpose:** Expose Metabase data as MCP resources
- **Examples:**
  - `metabase://database/{id}` - Database metadata
  - `metabase://dashboard/{id}` - Dashboard configuration
  - `metabase://card/{id}` - Question/card details

#### 3.3 MCP Protocol Implementation

##### 3.3.1 Tool Execution Flow
```
1. Client connects via SSE to /sse
2. Client sends tool call request (JSON-RPC 2.0 format)
3. Server validates tool name and parameters
4. Server executes corresponding Metabase API call
5. Server transforms response to MCP format
6. Server sends response via SSE
7. Connection remains open for subsequent requests
```

##### 3.3.2 Error Handling
- Invalid tool names → Return MCP error response
- Authentication failures → Return 401 with error message
- API errors → Map to appropriate MCP error codes
- Validation errors → Return 400 with detailed message
- Network timeouts → Retry logic with exponential backoff

##### 3.3.3 Security Considerations
- Validate all input parameters
- Sanitize user inputs before API calls
- Implement rate limiting per connection
- Log all tool executions for audit
- Support API key rotation

#### 3.4 Testing Strategy

##### Unit Tests
- **Package:** `src/test/java/io/veeblefetzer/remote_mcp_server/tools/`
- Test each tool definition in isolation
- Mock Metabase API client
- Verify input validation
- Check output formatting

##### Integration Tests
- **Package:** `src/test/java/io/veeblefetzer/remote_mcp_server/integration/`
- Test complete flow with real Metabase instance
- Use Testcontainers for isolated environment
- Verify SSE connection handling
- Test error scenarios
- Validate MCP protocol compliance

##### End-to-End Tests
- Test with actual MCP client (e.g., Cline)
- Verify all tools work as expected
- Test resource access
- Performance testing under load

#### 3.5 Documentation

##### API Documentation
- **File:** `docs/MCP_TOOLS.md`
- List all available tools
- Document input schemas
- Provide example requests and responses
- Include common use cases

##### Developer Guide
- **File:** `docs/DEVELOPMENT.md`
- Setup instructions
- Architecture overview
- Adding new tools guide
- Testing guidelines
- Deployment instructions

### Implementation Tasks

#### 3.1 Core MCP Infrastructure
- [ ] Create `McpServerController` with SSE endpoint
- [ ] Implement MCP protocol message parsing
- [ ] Set up JSON-RPC 2.0 request/response handling
- [ ] Configure Spring AI MCP dependencies
- [ ] Implement health check endpoint

#### 3.2 Tool Definitions (Prioritized by Importance)
- [ ] Define tool: `list-databases` - List all databases
- [ ] Define tool: `get-database` - Get database details
- [ ] Define tool: `execute-query` - Run native query
- [ ] Define tool: `list-dashboards` - List all dashboards
- [ ] Define tool: `get-dashboard` - Get dashboard details
- [ ] Define tool: `list-cards` - List questions/cards
- [ ] Define tool: `get-card` - Get card details
- [ ] Define tool: `create-card` - Create new question
- [ ] Define tool: `get-current-user` - Get current user info
- [ ] Define tool: `list-collections` - List collections

#### 3.3 Tool Executor Implementation
- [ ] Create `MetabaseToolExecutor` service
- [ ] Implement tool name to API method routing
- [ ] Add parameter validation logic
- [ ] Implement response transformation
- [ ] Add comprehensive error handling
- [ ] Implement logging for all operations

#### 3.4 Resource Implementation
- [ ] Define resource URI scheme
- [ ] Implement resource resolver
- [ ] Create resource for databases
- [ ] Create resource for dashboards
- [ ] Create resource for cards

#### 3.5 Testing
- [ ] Write unit tests for each tool (10 tests minimum)
- [ ] Write integration tests for SSE connection
- [ ] Write integration tests for tool execution flow
- [ ] Create Testcontainers configuration for isolated testing
- [ ] Write end-to-end test suite
- [ ] Achieve minimum 80% code coverage

#### 3.6 Documentation
- [ ] Create `docs/MCP_TOOLS.md` with tool reference
- [ ] Create `docs/DEVELOPMENT.md` with setup guide
- [ ] Add inline code documentation (Javadoc)
- [ ] Update main README.md with usage examples
- [ ] Create example client scripts

#### 3.7 Final Integration
- [ ] Test complete flow with Docker Compose environment
- [ ] Verify all tools work against real Metabase
- [ ] Performance testing (handle 10 concurrent connections)
- [ ] Security audit (input validation, error messages)
- [ ] Final code cleanup and refactoring

### Acceptance Criteria

✓ MCP server responds to SSE connections at `/sse` endpoint  
✓ All defined tools are functional and accessible  
✓ Each tool has comprehensive unit tests (100% coverage for tool logic)  
✓ Integration tests pass with real Metabase instance  
✓ Error handling covers all failure scenarios  
✓ MCP protocol compliance verified  
✓ Code is clean, well-documented, and follows best practices  
✓ All tests pass: `mvn test`  
✓ Project compiles: `mvn clean package`  
✓ End-to-end testing successful with MCP client  
✓ Documentation is complete and accurate  
✓ Conventional commit created: `feat: implement MCP server with SSE and Metabase tools`

---

## Commit Strategy

### Conventional Commit Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Commit Types
- `feat`: New feature implementation
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `chore`: Maintenance tasks
- `perf`: Performance improvements

### Example Commits

**Phase 1:**
```
feat(infrastructure): add Docker Compose infrastructure for Metabase and PostgreSQL

- Add docker-compose.yml with Metabase and PostgreSQL services
- Create database initialization scripts with sample data
- Add comprehensive setup documentation
- Configure health checks and service dependencies

Closes #1
```

**Phase 2:**
```
feat(client): configure OpenAPI Generator for Metabase API client

- Configure openapi-generator-maven-plugin in pom.xml
- Set up proper package structure for generated code
- Create MetabaseClientConfig for API client setup
- Add integration test for client verification

Closes #2
```

**Phase 3:**
```
feat(mcp): implement MCP server with SSE and Metabase tools

- Create McpServerController with SSE endpoint support
- Implement 10 core Metabase tools (database, query, dashboard operations)
- Add MetabaseToolExecutor service with comprehensive error handling
- Create resource definitions for databases, dashboards, and cards
- Add comprehensive test suite (unit, integration, e2e)
- Complete documentation for tools and development

Closes #3
```

---

## Project Milestones

### Milestone 1: Infrastructure Ready (Week 1)
- Docker environment operational
- Metabase accessible and configured
- Sample data available for testing

### Milestone 2: API Client Generated (Week 1-2)
- OpenAPI client successfully generated
- Configuration classes implemented
- Basic API operations verified

### Milestone 3: MCP Server MVP (Week 2-3)
- Core MCP infrastructure operational
- 5 essential tools implemented
- Basic testing in place

### Milestone 4: Complete Implementation (Week 3-4)
- All planned tools implemented
- Comprehensive test coverage
- Full documentation
- Production-ready code

---

## Risk Management

### Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| OpenAPI spec incomplete or invalid | High | Review and validate spec early; extend manually if needed |
| Spring AI MCP library compatibility issues | High | Research library before implementation; have fallback SSE implementation |
| Metabase API authentication complexity | Medium | Study Metabase auth docs; implement robust error handling |
| SSE connection stability | Medium | Implement reconnection logic; add connection monitoring |
| Performance with multiple concurrent clients | Medium | Load testing early; implement connection pooling |

### Process Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Scope creep in tool implementation | Medium | Prioritize essential tools; defer nice-to-haves |
| Insufficient testing coverage | High | Enforce 80% coverage minimum; automated checks |
| Documentation falling behind code | Medium | Document as you code; include in DoD |

---

## Success Criteria

### Functional Requirements
- [ ] MCP server successfully handles SSE connections
- [ ] All core Metabase operations accessible via MCP tools
- [ ] Resources properly exposed for context
- [ ] Error handling comprehensive and user-friendly

### Non-Functional Requirements
- [ ] Response time < 2s for typical operations
- [ ] Support 10+ concurrent SSE connections
- [ ] 80%+ code coverage
- [ ] Zero critical security vulnerabilities
- [ ] Complete and accurate documentation

### Business Requirements
- [ ] AI assistants can query Metabase data
- [ ] AI assistants can create and modify dashboards
- [ ] Solution deployable via Docker
- [ ] Easy to extend with new tools

---

## Post-Implementation

### Future Enhancements
1. **Phase 4: Advanced Features**
   - Caching layer for frequently accessed data
   - WebSocket support as alternative to SSE
   - Batch operations for multiple tool calls
   - Real-time dashboard updates

2. **Phase 5: Production Readiness**
   - Monitoring and alerting integration
   - Distributed tracing
   - Load balancing support
   - OAuth2 authentication

3. **Phase 6: Extended Functionality**
   - Support for Metabase pulse/alerts
   - SQL query building assistance
   - Dashboard template library
   - Multi-tenant support

### Maintenance Plan
- Regular dependency updates
- Metabase API compatibility testing
- Security vulnerability scanning
- Performance monitoring

---

## Appendix

### Key Dependencies
```xml
<!-- Already in pom.xml -->
- spring-boot-starter-web
- spring-boot-starter-test
- openapi-generator-maven-plugin

<!-- To be verified/added -->
- spring-ai-mcp-server (or equivalent)
- spring-boot-starter-webflux (for SSE)
- testcontainers (for integration tests)
```

### Useful Resources
- [Model Context Protocol Specification](https://modelcontextprotocol.io)
- [Metabase API Documentation](https://www.metabase.com/docs/latest/api-documentation)
- [OpenAPI Generator Documentation](https://openapi-generator.tech)
- [Spring Boot SSE Documentation](https://spring.io/guides/gs/serving-web-content/)
- [Conventional Commits](https://www.conventionalcommits.org)

### Contact & Support
- Project Repository: [GitHub Repository URL]
- Issue Tracker: [GitHub Issues URL]
- Documentation: [Project Documentation URL]

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Status:** Ready for Implementation
