# Metabase MCP Resources Reference

This document describes the MCP resources available for reading Metabase configuration and metadata.

## Overview

Resources provide read-only access to Metabase data that AI assistants can use as context. Unlike tools, resources are passive - they provide information without performing actions.

## URI Scheme

All resources use the `metabase://` URI scheme.

## Available Resources

| URI Template | Name | Description |
|-------------|------|-------------|
| `metabase://database/{id}` | Database Metadata | Provides metadata about a database |
| `metabase://database/{id}/tables` | Database Tables | Lists tables with schemas and fields |
| `metabase://dashboard/{id}` | Dashboard Configuration | Full dashboard configuration |
| `metabase://card/{id}` | Card Definition | Question definition and settings |
| `metabase://collection/{id}` | Collection Contents | Items in a collection |

---

## Resource Reference

### Database Metadata

**URI:** `metabase://database/{id}`

Provides metadata about a Metabase database, including name, engine type, and connection status.

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
  "description": "A sample PostgreSQL database",
  "is_sample": true,
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-02T00:00:00Z"
}
```

---

### Database Tables

**URI:** `metabase://database/{id}/tables`

Lists all tables in a database with their schemas, including column names, types, and relationships.

**Example:**
```
metabase://database/1/tables
```

**Response:**
```json
{
  "database_id": 1,
  "tables": [
    {
      "id": 1,
      "name": "users",
      "schema": "public",
      "display_name": "Users",
      "description": "User accounts",
      "fields": [
        {
          "id": 1,
          "name": "id",
          "display_name": "ID",
          "base_type": "type/Integer",
          "semantic_type": "type/PK"
        },
        {
          "id": 2,
          "name": "email",
          "display_name": "Email",
          "base_type": "type/Text",
          "semantic_type": "type/Email"
        }
      ]
    },
    {
      "id": 2,
      "name": "orders",
      "schema": "public",
      "display_name": "Orders",
      "description": "Customer orders",
      "fields": [...]
    }
  ]
}
```

---

### Dashboard Configuration

**URI:** `metabase://dashboard/{id}`

Provides the complete configuration of a dashboard, including layout, cards, and filter settings.

**Example:**
```
metabase://dashboard/1
```

**Response:**
```json
{
  "id": 1,
  "name": "Sales Dashboard",
  "description": "Monthly sales overview",
  "collection_id": 5,
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-02T00:00:00Z",
  "dashcards": [
    {
      "id": 1,
      "card_id": 10,
      "row": 0,
      "col": 0,
      "size_x": 6,
      "size_y": 4
    },
    {
      "id": 2,
      "card_id": 11,
      "row": 0,
      "col": 6,
      "size_x": 6,
      "size_y": 4
    }
  ],
  "parameters": []
}
```

---

### Card Definition

**URI:** `metabase://card/{id}`

Provides the definition of a saved question (card), including the query, visualization settings, and description.

**Example:**
```
metabase://card/10
```

**Response:**
```json
{
  "id": 10,
  "name": "Monthly Revenue",
  "description": "Total revenue by month",
  "database_id": 1,
  "collection_id": 5,
  "query_type": "native",
  "display": "line",
  "dataset_query": {
    "database": 1,
    "type": "native",
    "native": {
      "query": "SELECT date_trunc('month', created_at) as month, SUM(amount) as revenue FROM orders GROUP BY 1"
    }
  },
  "visualization_settings": {
    "graph.dimensions": ["month"],
    "graph.metrics": ["revenue"]
  },
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-02T00:00:00Z"
}
```

---

### Collection Contents

**URI:** `metabase://collection/{id}`

Lists all items in a collection, including dashboards, questions, and sub-collections.

**Example:**
```
metabase://collection/5
```

**Response:**
```json
{
  "collection_id": "5",
  "items": [
    {
      "id": 1,
      "name": "Sales Dashboard",
      "model": "dashboard",
      "description": "Monthly sales overview"
    },
    {
      "id": 10,
      "name": "Monthly Revenue",
      "model": "card",
      "description": "Total revenue by month"
    }
  ],
  "total": 2
}
```

**Note:** Use `root` as the collection ID to access the root collection:
```
metabase://collection/root
```

---

## Usage with MCP Protocol

### List Resources

To list available resource templates, send a `resources/list` request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "resources/list",
  "params": {}
}
```

### Read Resource

To read a specific resource, send a `resources/read` request:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "resources/read",
  "params": {
    "uri": "metabase://database/1"
  }
}
```

---

## Error Handling

Resources return errors in a consistent format:

```json
{
  "error": "Database not found: 999"
}
```

Common errors:
- Resource not found
- Invalid URI format
- API authentication failure
- Permission denied

---

## Best Practices

1. **Use resources for context** - Resources are ideal for providing context to AI assistants about available data and configurations.

2. **Cache resource data** - Resource data changes infrequently, so consider caching results for better performance.

3. **Start with database tables** - Use `metabase://database/{id}/tables` to understand the schema before writing queries.

4. **Combine with tools** - Use resources to explore available data, then use tools to execute queries and create cards.
