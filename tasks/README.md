# Metabase Remote MCP Server - Tasks

This directory contains executable tasks for implementing the Metabase Remote MCP Server. Each task is designed to be self-contained with clear objectives, requirements, and acceptance criteria.

## Task Overview

| Phase | Task ID | Title | Status | Dependencies |
|-------|---------|-------|--------|--------------|
| 1 | 1.1 | Docker Compose Configuration | ⬜ | None |
| 1 | 1.2 | Sample Data Population | ⬜ | 1.1 |
| 1 | 1.3 | Infrastructure Documentation | ⬜ | 1.1, 1.2 |
| 2 | 2.1 | Maven Plugin Configuration | ⬜ | Phase 1 |
| 2 | 2.2 | Client Configuration Class | ⬜ | 2.1 |
| 2 | 2.3 | Client Testing | ⬜ | 2.2 |
| 3 | 3.1 | Core MCP Infrastructure | ⬜ | Phase 2 |
| 3 | 3.2 | Tool Definitions | ⬜ | 3.1 |
| 3 | 3.3 | Tool Executor Implementation | ⬜ | 3.2 |
| 3 | 3.4 | Resource Implementation | ⬜ | 3.1 |
| 3 | 3.5 | Testing Suite | ⬜ | 3.3, 3.4 |
| 3 | 3.6 | MCP Documentation | ⬜ | 3.5 |

**Status Legend:** ⬜ Not Started | 🔄 In Progress | ✅ Complete

## Directory Structure

```
tasks/
├── README.md                           # This file
├── phase-1-infrastructure/
│   ├── TASK-1.1-docker-compose.md     # Docker Compose setup
│   ├── TASK-1.2-sample-data.md        # Database schema & sample data
│   └── TASK-1.3-documentation.md      # Docker documentation
├── phase-2-openapi-client/
│   ├── TASK-2.1-maven-plugin.md       # OpenAPI Generator configuration
│   ├── TASK-2.2-client-config.md      # Metabase client configuration
│   └── TASK-2.3-client-testing.md     # Integration tests
└── phase-3-mcp-server/
    ├── TASK-3.1-core-infrastructure.md # SSE endpoint & MCP protocol
    ├── TASK-3.2-tool-definitions.md    # MCP tool definitions
    ├── TASK-3.3-tool-executor.md       # Tool executor service
    ├── TASK-3.4-resources.md           # MCP resources
    ├── TASK-3.5-testing.md             # Test suite
    └── TASK-3.6-documentation.md       # Documentation
```

## Dependency Graph

```
Phase 1: Infrastructure Setup
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   [1.1 Docker Compose] ───┬───► [1.2 Sample Data]          │
│                           │                                 │
│                           └───► [1.3 Documentation] ◄──────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
Phase 2: OpenAPI Client Generation
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   [2.1 Maven Plugin] ───► [2.2 Client Config] ───► [2.3]   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
Phase 3: MCP Server Implementation
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   [3.1 Core Infrastructure] ───┬───► [3.2 Tool Defs]       │
│                                │           │                │
│                                │           ▼                │
│                                │     [3.3 Tool Executor]   │
│                                │           │                │
│                                └───► [3.4 Resources]       │
│                                            │                │
│                                            ▼                │
│                                      [3.5 Testing]         │
│                                            │                │
│                                            ▼                │
│                                   [3.6 Documentation]      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Getting Started

1. Start with **Phase 1** tasks in order (1.1 → 1.2 → 1.3)
2. Move to **Phase 2** after infrastructure is ready
3. Complete **Phase 3** for MCP server implementation

## GitHub Issues

- Phase 1: [#1](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/1)
- Phase 2: [#2](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/2)
- Phase 3: [#3](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/3)

## Task File Format

Each task file follows this structure:

1. **Header** - Task ID, title, and GitHub issue reference
2. **Objective** - Clear goal for the task
3. **Prerequisites** - What must be completed first
4. **Technical Details** - Specific implementation requirements
5. **Implementation Checklist** - Step-by-step actions
6. **Acceptance Criteria** - Definition of done
7. **Commit Template** - Ready-to-use conventional commit message

## Workflow

For each task:

1. **Plan** - Read and understand the task requirements
2. **Implement** - Complete the implementation checklist
3. **Test** - Verify all acceptance criteria are met
4. **Compile** - Ensure project builds: `mvn clean compile`
5. **Commit** - Use the provided commit template
6. **Update Status** - Mark task as complete in this README
