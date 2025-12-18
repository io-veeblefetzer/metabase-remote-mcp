# Metabase Remote MCP Server

A Model Context Protocol (MCP) server that enables AI assistants to interact with Metabase, providing tools for querying databases, managing dashboards, and exploring data.

## Features

- **12 MCP Tools** for comprehensive Metabase interaction
- **5 MCP Resources** for read-only access to Metabase metadata
- **SSE Transport** for remote connections via HTTP
- **Docker Compose** development environment with Metabase and sample database

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (for development environment)

### 1. Start Development Environment

```bash
# Start Metabase with sample database
docker compose up -d

# Wait for Metabase to be ready (first start takes ~2 minutes)
docker compose logs -f metabase
```

Access Metabase at http://localhost:3000 and complete the initial setup.

### 2. Create Metabase API Key

1. Log into Metabase
2. Go to **Settings** > **Admin settings** > **API Keys**
3. Create a new API key with appropriate permissions
4. Copy the API key

### 3. Configure and Run

```bash
# Set environment variables
export METABASE_URL=http://localhost:3000
export METABASE_API_KEY=your-api-key-here

# Build and run
mvn clean compile
mvn spring-boot:run
```

The MCP server will be available at `http://localhost:8080`.

## Configuration

Configure via environment variables or `application.properties`:

| Variable | Default | Description |
|----------|---------|-------------|
| `METABASE_URL` | `http://localhost:3000` | Metabase instance URL |
| `METABASE_API_KEY` | (required) | Metabase API key |
| `metabase.connect-timeout` | `10s` | Connection timeout |
| `metabase.read-timeout` | `30s` | Read timeout |
| `metabase.max-connections` | `10` | Connection pool size |

## Available MCP Tools

| Tool | Description |
|------|-------------|
| `list-databases` | List all connected databases |
| `get-database` | Get database details and metadata |
| `execute-query` | Execute SQL queries |
| `list-dashboards` | List all dashboards |
| `get-dashboard` | Get dashboard configuration |
| `list-cards` | List saved questions |
| `get-card` | Get question definition |
| `create-card` | Create new saved question |
| `run-card-query` | Execute saved question |
| `get-current-user` | Get authenticated user info |
| `list-collections` | List collections |
| `get-collection-items` | Get items in a collection |
| `get-table-metadata` | Get table schema and fields |

See [docs/MCP_TOOLS.md](docs/MCP_TOOLS.md) for detailed documentation.

## Available MCP Resources

| URI Template | Description |
|--------------|-------------|
| `metabase://database/{id}` | Database metadata |
| `metabase://database/{id}/tables` | Tables and schemas |
| `metabase://dashboard/{id}` | Dashboard configuration |
| `metabase://card/{id}` | Question definition |
| `metabase://collection/{id}` | Collection contents |

See [docs/MCP_RESOURCES.md](docs/MCP_RESOURCES.md) for detailed documentation.

## Usage Examples

### List Available Databases

```json
{
  "name": "list-databases",
  "arguments": {}
}
```

### Execute a Query

```json
{
  "name": "execute-query",
  "arguments": {
    "databaseId": 1,
    "query": "SELECT * FROM customers LIMIT 10"
  }
}
```

### Create a New Question

```json
{
  "name": "create-card",
  "arguments": {
    "name": "Monthly Revenue",
    "databaseId": 1,
    "query": "SELECT date_trunc('month', created_at) as month, SUM(amount) FROM orders GROUP BY 1",
    "visualizationType": "line"
  }
}
```

## Development

### Project Structure

```
src/main/java/io/veeblefetzer/remote_mcp_server/
├── config/                 # Configuration classes
│   ├── MetabaseClientConfig.java
│   └── MetabaseProperties.java
├── mcp/                    # MCP server configuration
│   ├── McpServerConfig.java
│   └── model/
├── tools/                  # MCP tool implementations
│   └── MetabaseToolService.java
└── resources/              # MCP resource implementations
    └── MetabaseResourceService.java
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MetabaseToolServiceTest
```

### Building

```bash
# Build JAR
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

## Docker Environment

The development environment includes:

- **Metabase** (port 3000) - Business intelligence platform
- **PostgreSQL** (port 5432) - Metabase's internal database
- **Sample Database** (port 5433) - PostgreSQL with sample e-commerce data

### Sample Database Schema

The sample database includes:
- `customers` - Customer information
- `products` - Product catalog
- `orders` - Order records
- `order_items` - Order line items

### Docker Commands

```bash
# Start environment
docker compose up -d

# View logs
docker compose logs -f

# Stop environment
docker compose down

# Reset everything (including volumes)
docker compose down -v
```

## Integration with Claude Desktop

Add to your Claude Desktop configuration (`~/.config/claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "metabase": {
      "url": "http://localhost:8080/mcp/message"
    }
  }
}
```

## Security Considerations

- **API Keys**: Store API keys securely, never commit them to version control
- **Query Execution**: Queries run with the permissions of the API key user
- **Network**: Consider using HTTPS in production
- **Access Control**: Use Metabase's permission system to restrict data access

## License

MIT License - see [LICENSE](LICENSE) for details.

## Documentation

- [MCP Tools Reference](docs/MCP_TOOLS.md)
- [MCP Resources Reference](docs/MCP_RESOURCES.md)
- [Docker Setup](docker/README.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
