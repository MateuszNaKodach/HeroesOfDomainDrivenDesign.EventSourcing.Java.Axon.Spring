# MCP Server Implementation Plan

## Context

This task involves implementing an MCP (Model Context Protocol) server for the Heroes of Might & Magic III domain application. The application is built using Domain-Driven Design with Event Sourcing, CQRS, and Vertical Slice Architecture using Axon Framework and Spring Boot.

The MCP server will expose the rich domain model through standardized MCP resources, tools, and prompts, making it valuable for:
- Game development and testing
- DDD/Event Sourcing education
- AI-assisted game strategy analysis
- Domain modeling demonstrations

## Implementation Approach

**🎯 Start Small, Scale Up**: We will begin with a single bounded context (**Creature Recruitment**) and implement one complete slice (`creaturerecruitment/write/builddwelling`) as a proof of concept. Once this foundation is solid, we'll expand to other slices within the same bounded context, then replicate the pattern across other bounded contexts.

## Phase 1: General Plan

### MCP Server Capabilities Overview

**Resources (Read-Only Data Access)**
- Game state resources (dwellings, armies, calendar)
- Domain knowledge schemas (creatures, resources, commands)
- Event streams for analysis and debugging

**Tools (Interactive Operations)**
- Game management (create, day progression)
- Dwelling operations (build, recruit creatures)
- Army management (add/remove creatures)
- Resource management (deposit/withdraw)
- Analysis tools (game state, validation, cost calculation)

**Prompts (Domain Intelligence)**
- Game strategy and planning
- Educational DDD/Event Sourcing concepts
- Testing scenario generation
- Game balance analysis
- Maintenance and debugging

## Phase 2: Implementation TODOs

### 🏗️ Core Infrastructure (Foundation)
- [x] Add Spring AI MCP Server dependencies
- [x] Configure MCP server in Spring Boot application
- [x] Create base MCP configuration classes in `shared/mcp/`
- [ ] Implement authentication/authorization for MCP

### 🚀 Phase 1: Proof of Concept (Start Here)
**Target: `creaturerecruitment/write/builddwelling` slice**
- [x] **creaturerecruitment/write/builddwelling**: Build dwelling tools/resources
- [x] Test and validate the first slice implementation
- [x] Establish patterns and conventions

### 📈 Phase 2: Expand Creature Recruitment Context
- [ ] **creaturerecruitment/write/recruitcreature**: Recruit creature tools/resources  
- [ ] **creaturerecruitment/write/changeavailablecreatures**: Availability tools/resources
- [ ] **creaturerecruitment/read/getdwellingbyid**: Single dwelling query resources
- [ ] **creaturerecruitment/read/getalldwellings**: Dwellings list query resources
- [ ] **creaturerecruitment**: Shared recruitment strategy prompts

### 🔄 Phase 3: Replicate to Other Contexts
- [ ] **armies/write/addcreature**: Add creature to army tools
- [ ] **armies/write/removecreature**: Remove creature from army tools
- [ ] **armies**: Army composition and strategy prompts
- [ ] **calendar/write/startday**: Start day progression tools
- [ ] **calendar/write/finishday**: Finish day progression tools
- [ ] **calendar**: Time management and planning prompts
- [ ] **resourcespool/write/deposit**: Resource deposit tools
- [ ] **resourcespool/write/withdraw**: Resource withdrawal tools
- [ ] **resourcespool**: Resource optimization prompts

### 🔧 Phase 4: Advanced Features
- [ ] **maintenance/read/geteventstream**: Event stream query resources
- [ ] **maintenance/write/resetprocessor**: Processor management tools
- [ ] Cross-context analysis prompts
- [ ] Game balance and optimization prompts
- [ ] Educational DDD/Event Sourcing prompts

### ✅ Integration & Testing
- [ ] Integration tests for MCP endpoints
- [ ] Documentation and examples
- [ ] Performance optimization

## Phase 3: Technology Stack

### Core Dependencies
- **Spring Boot** (existing)
- **Spring AI MCP Server**: `spring-ai-starter-mcp-server-webmvc`
- **Axon Framework** (existing - for command/query handling)
- **Spring Data JPA** (existing - for read models)

### MCP-Specific Components
- MCP Resource providers
- MCP Tool handlers
- MCP Prompt processors
- JSON Schema generators for domain objects

## Phase 4: Architecture

### Vertical Slice Architecture Alignment

Each slice will contain its own MCP components in `ModelContextProtocol.java` files:

```
com.dddheroes.heroesofddd/
├── creaturerecruitment/
│   ├── write/
│   │   ├── builddwelling/
│   │   │   └── ModelContextProtocol.java    # Build dwelling tools/resources
│   │   ├── recruitcreature/
│   │   │   └── ModelContextProtocol.java    # Recruit creature tools/resources
│   │   └── changeavailablecreatures/
│   │       └── ModelContextProtocol.java    # Availability management tools
│   ├── read/
│   │   ├── getdwellingbyid/
│   │   │   └── ModelContextProtocol.java    # Dwelling query resources
│   │   └── getalldwellings/
│   │       └── ModelContextProtocol.java    # Dwellings list resources
│   └── ModelContextProtocol.java            # Shared recruitment prompts
├── armies/
│   ├── write/
│   │   ├── addcreature/
│   │   │   └── ModelContextProtocol.java    # Add creature tools
│   │   └── removecreature/
│   │       └── ModelContextProtocol.java    # Remove creature tools
│   └── ModelContextProtocol.java            # Army strategy prompts
├── calendar/
│   ├── write/
│   │   ├── startday/
│   │   │   └── ModelContextProtocol.java    # Start day tools
│   │   └── finishday/
│   │       └── ModelContextProtocol.java    # Finish day tools
│   └── ModelContextProtocol.java            # Time management prompts
├── resourcespool/
│   ├── write/
│   │   ├── deposit/
│   │   │   └── ModelContextProtocol.java    # Deposit tools
│   │   └── withdraw/
│   │       └── ModelContextProtocol.java    # Withdraw tools
│   └── ModelContextProtocol.java            # Resource strategy prompts
├── maintenance/
│   ├── read/geteventstream/
│   │   └── ModelContextProtocol.java        # Event stream resources
│   └── write/resetprocessor/
│       └── ModelContextProtocol.java        # Processor management tools
└── shared/
    └── mcp/
        ├── configuration/                   # Base MCP config
        ├── schemas/                         # Domain schemas
        └── security/                        # Authentication
```

### MCP Integration Points
- **Resources**: Connect to existing read models and query handlers
- **Tools**: Leverage existing command handlers and REST APIs
- **Prompts**: Use domain services and business logic
- **Security**: Integrate with existing authentication mechanisms

### Design Principles
- Maintain bounded context isolation
- Reuse existing command/query infrastructure
- Follow existing domain patterns
- Ensure event sourcing audit trails
- Preserve business rule validation

## Implementation Strategy

### 🎯 Incremental Development Approach

1. **🏗️ Foundation First**: Establish shared MCP infrastructure and configuration
2. **🚀 Proof of Concept**: Implement single slice (`creaturerecruitment/write/builddwelling`) to validate approach
3. **📈 Context Completion**: Complete all slices within Creature Recruitment bounded context
4. **🔄 Pattern Replication**: Apply proven patterns to other bounded contexts (armies, calendar, etc.)
5. **🔧 Advanced Features**: Add cross-context analysis, educational prompts, and optimization tools

### 🎉 Success Criteria

Each phase completion should result in:
- **Working MCP endpoints** for the implemented slices
- **Comprehensive testing** of tools/resources/prompts
- **Documentation** of patterns and conventions
- **Performance validation** under realistic load

This incremental approach ensures the MCP server integrates seamlessly with the existing domain architecture while providing rich, domain-aware capabilities for external consumers. Starting with a single slice allows us to establish solid foundations and patterns that can be confidently replicated across the entire application.

## 📚 Implementation Patterns & Documentation

### MCP Tool Implementation Pattern

Based on the successful implementation of `build_dwelling` tool, here's the established pattern for creating MCP tools:

#### 1. **File Location**
- Each slice gets its own `ModelContextProtocol.java` file
- Location: `{context}/write/{slice}/ModelContextProtocol.java`
- Example: `creaturerecruitment/write/builddwelling/ModelContextProtocol.java`

#### 2. **Class Structure**
```java
@Component
public class ModelContextProtocol {
    
    private final CommandGateway commandGateway;
    
    public ModelContextProtocol(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }
    
    @Tool(
        name = "tool_name",
        description = "Detailed description of what the tool does"
    )
    public CompletableFuture<Map<String, Object>> methodName(
        @ToolParam(description = "Parameter description") String param1,
        // ... more parameters
    ) {
        // Implementation
    }
}
```

#### 3. **Key Design Decisions**
- **Use Spring AI `@Tool`** annotation (not `@McpTool`)
- **Return `CompletableFuture<Map<String, Object>>`** for async operation results
- **Include `@ToolParam` descriptions** for all parameters to help AI understand usage
- **Reuse existing Command/CommandGateway** infrastructure
- **Follow GameMetaData pattern** for context (gameId, playerId)
- **Provide structured success/error responses** with consistent format

#### 4. **Parameter Validation Pattern**
```java
@ToolParam(description = "Unique identifier for the game instance") String gameId,
@ToolParam(description = "Unique identifier for the player") String playerId,
@ToolParam(description = "Unique identifier for the dwelling to build") String dwellingId,
@ToolParam(description = "Type of creature this dwelling will recruit (e.g., 'ANGEL', 'DRAGON', 'GRIFFIN')") String creatureId,
@ToolParam(description = "Resource cost per troop recruitment. Map of resource types to amounts (e.g., {'GOLD': 1000, 'WOOD': 10})") Map<String, Integer> costPerTroop
```

#### 5. **Response Pattern**
```java
// Success Response
return commandGateway.send(command, GameMetaData.with(gameId, playerId))
    .thenApply(result -> Map.of(
        "success", true,
        "dwellingId", dwellingId,
        "creatureId", creatureId,
        "costPerTroop", costPerTroop,
        "message", "Dwelling built successfully"
    ))
    .exceptionally(throwable -> Map.of(
        "success", false,
        "error", throwable.getMessage(),
        "dwellingId", dwellingId,
        "message", "Failed to build dwelling: " + throwable.getMessage()
    ));
```

### Next Implementation Steps

With the proven pattern established, implement remaining tools following the same structure:

1. **creaturerecruitment/write/recruitcreature/ModelContextProtocol.java**
2. **creaturerecruitment/write/changeavailablecreatures/ModelContextProtocol.java** 
3. **creaturerecruitment/read/getdwellingbyid/ModelContextProtocol.java** (Resources)
4. **creaturerecruitment/read/getalldwellings/ModelContextProtocol.java** (Resources)

Each implementation should:
- Follow the established class structure
- Use appropriate parameter validation
- Maintain consistent response formats
- Leverage existing domain infrastructure