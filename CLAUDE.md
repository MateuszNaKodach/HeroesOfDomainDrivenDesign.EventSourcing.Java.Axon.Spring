# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Building and Running
- `./mvnw clean install` - Clean build with all tests
- `./mvnw install -DskipTests` - Fast build without tests
- `./mvnw spring-boot:run` - Run the application locally
- `./mvnw test` - Run all tests
- `./mvnw test -Dtest=ClassName` - Run specific test class
- `./mvnw test -Dtest=ClassName#methodName` - Run specific test method

### Infrastructure
- `docker compose up` - Start Axon Server and PostgreSQL containers
- `docker compose down` - Stop containers
- Application runs on default Spring Boot port (8080)
- Axon Server UI available at http://localhost:8024
- PostgreSQL accessible at localhost:6446

### Testing
- Tests use real PostgreSQL Event Store via Testcontainers
- All tests follow BDD pattern: given(events) → when(command) → then(events/state)
- Use `AxonUtils` and `EventStoreAssertions` helper classes for test setup

## Architecture Overview

This is a Domain-Driven Design implementation using Event Sourcing with Axon Framework and Spring Modulith. The application models a Heroes of Might & Magic III domain with four main bounded contexts.

### Core Architectural Patterns
- **Event Sourcing**: All state changes captured as domain events
- **CQRS**: Separate write (command) and read (query) models
- **Vertical Slice Architecture**: Features organized by capability, not technical layer
- **Spring Modulith**: Module boundaries enforced at compile time
- **Event Modeling**: System design follows Event Modeling nomenclature

### Module Structure
Each domain module follows a consistent structure with three slice types:

```
com.dddheroes.heroesofddd/
├── armies/              # Army management
├── astrologers/         # Week symbols and creature bonuses
├── calendar/           # Game time progression
├── creaturerecruitment/ # Building dwellings and recruiting creatures
├── resourcespool/      # Player resources management
├── maintenance/        # Event store operations
└── shared/            # Common domain concepts
```

### Slice Types (following Event Modeling)
- **write/**: Commands → Aggregates → Events (business logic)
- **read/**: Event projections → Read models → Queries (data access)
- **automation/**: Event processors implementing business policies
- **events/**: Domain events (contracts between slices)

### Key Aggregates
- `Army` - manages creature stacks (max 7)
- `Dwelling` - creature recruitment buildings
- `Calendar` - game time progression
- `Astrologers` - weekly creature bonuses
- `ResourcesPool` - player resources (gold, wood, etc.)

### Event Processing
- Uses Axon Framework event processors with game-specific sequencing
- All processors use `gameIdSequencingPolicy` for consistency
- Dead Letter Queue (DLQ) enabled for automation processors
- Event store backed by PostgreSQL with JPA

### Domain Rules Examples
- Armies limited to 7 creature stacks (`CanHaveMax7CreatureStacksInArmy`)
- Cannot recruit more creatures than available (`RecruitCreaturesNotExceedAvailableCreatures`)
- Days must be started in sequence (`CannotSkipDays`)
- One week symbol per week (`OnlyOneSymbolPerWeek`)

### Testing Strategy
Tests focus on observable behavior using Axon Test Fixture:
- Write slice tests: given(events) → when(command) → then(events)
- Read slice tests: given(events) → then(read model state)
- Automation tests: when(event) → then(command issued)

### REST API
- Swagger UI available at `/swagger-ui.html`
- API docs at `/api-docs`
- Maintenance endpoints for event stream operations

### Configuration Notes
- Axon Server disabled by default (uses JPA event store)
- Virtual threads enabled for better concurrency
- Jackson serialization for events and messages
- Game metadata correlation for event sequencing