# Metabase MCP Tools Reference

This document provides detailed information about all available MCP tools for interacting with Metabase.

## Overview

The Metabase MCP Server exposes 12 tools for database operations, querying, dashboards, cards, collections, and more.

## Tool Categories

### Database Operations
- [list-databases](#list-databases) - List all connected databases
- [get-database](#get-database) - Get database details

### Query Operations
- [execute-query](#execute-query) - Execute SQL queries

### Dashboard Operations
- [list-dashboards](#list-dashboards) - List all dashboards
- [get-dashboard](#get-dashboard) - Get dashboard details

### Card/Question Operations
- [list-cards](#list-cards) - List all saved questions
- [get-card](#get-card) - Get card details
- [create-card](#create-card) - Create a new question
- [run-card-query](#run-card-query) - Execute a saved question

### User Operations
- [get-current-user](#get-current-user) - Get authenticated user info

### Collection Operations
- [list-collections](#list-collections) - List collections
- [get-collection-items](#get-collection-items) - Get items in a collection

### Table Operations
- [get-table-metadata](#get-table-metadata) - Get table schema and fields

---

## Tool Reference

### list-databases

List all databases connected to Metabase.

**Description:** Returns database IDs, names, and connection details. Use this to discover available data sources.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| includeTables | boolean | No | Include table information in response (default: false) |

**Example:**
```json
{
  "name": "list-databases",
  "arguments": {
    "includeTables": true
  }
}
```

**Response:**
```json
{
  "databases": [
    {
      "id": 1,
      "name": "Sample Database",
      "engine": "postgres",
      "is_sample": true
    },
    {
      "id": 2,
      "name": "Production DB",
      "engine": "mysql",
      "is_sample": false
    }
  ]
}
```

---

### get-database

Get detailed information about a specific database.

**Description:** Returns connection settings and available tables for a database.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| databaseId | integer | Yes | The ID of the database to retrieve |
| includeMetadata | boolean | No | Include table and field metadata (default: false) |

**Example:**
```json
{
  "name": "get-database",
  "arguments": {
    "databaseId": 1,
    "includeMetadata": true
  }
}
```

---

### execute-query

Execute a native SQL query against a database.

**Description:** Returns query results as rows and columns. Use with caution - queries are executed directly.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| databaseId | integer | Yes | The database to query |
| query | string | Yes | SQL query to execute |
| limit | integer | No | Max rows to return (default: 100) |

**Example:**
```json
{
  "name": "execute-query",
  "arguments": {
    "databaseId": 1,
    "query": "SELECT * FROM customers WHERE country = 'USA'",
    "limit": 50
  }
}
```

**Response:**
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

**⚠️ Security Note:** Queries are executed with the permissions of the Metabase API key. Ensure proper access controls are in place.

---

### list-dashboards

List all dashboards in Metabase.

**Description:** Returns dashboard IDs, names, and descriptions. Use to discover available dashboards.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| collectionId | integer | No | Filter by collection ID |

**Example:**
```json
{
  "name": "list-dashboards",
  "arguments": {
    "collectionId": 5
  }
}
```

**Response:**
```json
{
  "dashboards": [
    {
      "id": 1,
      "name": "Sales Dashboard",
      "description": "Monthly sales overview",
      "collection_id": 5
    }
  ]
}
```

---

### get-dashboard

Get detailed information about a dashboard.

**Description:** Returns all cards (questions) and their configurations.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| dashboardId | integer | Yes | The ID of the dashboard to retrieve |

**Example:**
```json
{
  "name": "get-dashboard",
  "arguments": {
    "dashboardId": 1
  }
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Sales Dashboard",
  "description": "Monthly sales overview",
  "collection_id": 5,
  "cards": [
    {
      "id": 1,
      "card_id": 10,
      "size_x": 6,
      "size_y": 4,
      "row": 0,
      "col": 0
    }
  ]
}
```

---

### list-cards

List all saved questions (cards) in Metabase.

**Description:** Returns card IDs, names, and query types.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| collectionId | integer | No | Filter by collection ID |
| databaseId | integer | No | Filter by database ID |

**Example:**
```json
{
  "name": "list-cards",
  "arguments": {
    "databaseId": 1
  }
}
```

---

### get-card

Get detailed information about a saved question (card).

**Description:** Returns the query definition and visualization settings.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| cardId | integer | Yes | The ID of the card to retrieve |

**Example:**
```json
{
  "name": "get-card",
  "arguments": {
    "cardId": 10
  }
}
```

---

### create-card

Create a new saved question (card) in Metabase.

**Description:** Creates a new question with the specified query and visualization.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| name | string | Yes | Name of the question |
| databaseId | integer | Yes | Database to query |
| query | string | Yes | SQL query to execute |
| visualizationType | string | No | Visualization type (table, bar, line, pie, etc.) Default: table |
| collectionId | integer | No | Collection to save the card in |

**Example:**
```json
{
  "name": "create-card",
  "arguments": {
    "name": "Monthly Revenue",
    "databaseId": 1,
    "query": "SELECT date_trunc('month', created_at) as month, SUM(amount) as revenue FROM orders GROUP BY 1 ORDER BY 1",
    "visualizationType": "line",
    "collectionId": 5
  }
}
```

**Response:**
```json
{
  "id": 15,
  "name": "Monthly Revenue",
  "message": "Card created successfully"
}
```

---

### run-card-query

Execute a saved question (card) and return its results.

**Description:** Use this to get data from an existing saved question.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| cardId | integer | Yes | The ID of the card to execute |

**Example:**
```json
{
  "name": "run-card-query",
  "arguments": {
    "cardId": 10
  }
}
```

---

### get-current-user

Get information about the currently authenticated user.

**Description:** Returns name, email, and permissions.

**Parameters:** None

**Example:**
```json
{
  "name": "get-current-user",
  "arguments": {}
}
```

**Response:**
```json
{
  "id": 1,
  "email": "admin@example.com",
  "first_name": "Admin",
  "last_name": "User",
  "is_superuser": true
}
```

---

### list-collections

List all collections in Metabase.

**Description:** Collections organize dashboards and questions into folders.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| parentId | integer | No | Parent collection ID to filter by |

**Example:**
```json
{
  "name": "list-collections",
  "arguments": {}
}
```

---

### get-collection-items

Get all items in a collection.

**Description:** Returns dashboards, questions, and sub-collections.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| collectionId | string | Yes | The ID of the collection (use 'root' for root collection) |

**Example:**
```json
{
  "name": "get-collection-items",
  "arguments": {
    "collectionId": "root"
  }
}
```

**Response:**
```json
{
  "items": [
    {"id": 1, "name": "Dashboard 1", "model": "dashboard", "description": ""},
    {"id": 2, "name": "Query 1", "model": "card", "description": ""}
  ],
  "total": 2
}
```

---

### get-table-metadata

Get metadata for a specific table.

**Description:** Returns columns, types, and foreign key relationships.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| tableId | integer | Yes | The ID of the table to get metadata for |

**Example:**
```json
{
  "name": "get-table-metadata",
  "arguments": {
    "tableId": 5
  }
}
```

**Response:**
```json
{
  "id": 5,
  "name": "orders",
  "schema": "public",
  "db_id": 1,
  "fields": [
    {"id": 10, "name": "id", "display_name": "ID", "base_type": "type/Integer", "semantic_type": "type/PK"},
    {"id": 11, "name": "amount", "display_name": "Amount", "base_type": "type/Decimal", "semantic_type": ""}
  ]
}
```

---

## Error Handling

All tools return errors in a consistent format:

```json
{
  "error": "Error message describing what went wrong"
}
```

Common error codes:
- Missing required parameter
- Database/card/dashboard not found
- API authentication failure
- Query execution error

---

## Best Practices

1. **Start with list operations** - Use `list-databases`, `list-dashboards`, etc. to discover available resources before accessing them.

2. **Use appropriate limits** - When executing queries, set reasonable limits to avoid overwhelming the system.

3. **Check permissions** - Ensure the API key has appropriate permissions for the operations you want to perform.

4. **Handle errors gracefully** - Always check for error responses before processing results.

5. **Use collections for organization** - Organize your dashboards and questions into collections for easier management.
