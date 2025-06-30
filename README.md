# Heroes of Domain-Driven Design (Java)
Shows how to use Domain-Driven Design, Event Storming, Event Modeling and Event Sourcing in Heroes of Might & Magic III domain.

👉 See also implementations in:
- [Kotlin + Axon Framework 5 (DCB)](https://github.com/MateuszNaKodach/HeroesOfDomainDrivenDesign.EventSourcing.DCB.Kotlin.Axon5.Spring)
- [TypeScript + Emmett](https://github.com/MateuszNaKodach/HeroesOfDomainDrivenDesign.EventSourcing.TypeScript.Emmett.Express)
- [Ruby + RailsEventStore](https://github.com/MateuszNaKodach/HeroesOfDomainDrivenDesign.EventSourcing.Ruby)

👉 [Let's explore the Heroes of Domain-Driven Design blogpost series](https://dddheroes.com/)
- There you will get familiar with the whole Software Development process: from knowledge crunching with domain experts, designing solution using Event Modeling, to implementation using DDD Building Blocks.

This project probably won't be a fully functional HOMM3 engine implementation because it's done for educational purposes.
If you'd like to talk with me about mentioned development practices, feel free to contact on [linkedin.com/in/mateusznakodach/](https://www.linkedin.com/in/mateusznakodach).

I'm focused on domain modeling on the backend, but I'm going to implement UI like below in the future.

![Heroes3_CreatureRecruitment_ExampleGif](https://github.com/user-attachments/assets/0e503a1e-e5d2-4e4a-9150-1a224e603be8)

## 🚀 How to run the project locally?

0. Install Java 23 on your machine
1. `./mvnw install -DskipTests`
2. `docker compose up`
3. `./mvnw spring-boot:run` or `./mvnw test`

## 🧱 Modules

Modules (mostly designed using Bounded Context heuristic) are designed and documented on EventModeling below.
Each slice in a module is in certain color which shows the progress:
- green -> completed
- yellow -> implementation in progress
- red -> to do
- grey -> design in progress

List of modules you can see in package `com.dddheroes.heroesofddd`.
```
heroesofddd/
├── armies
├── astrologers
├── calendar
├── creature_recruitment
```

Each domain-focused module follows Vertical-Slice Architecture of three possible types: write, read and automation following Event Modeling nomenclature.

### 👾 Creature Recruitment

![EventModeling_Module_CreatureRecruitment.png](docs/images/EventModeling_Module_CreatureRecruitment.png)

Slices:
- Write: [BuildDwelling -> DwellingBuilt](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/write/builddwelling) | [test](src/test/java/com/dddheroes/heroesofddd/creaturerecruitment/write/builddwelling/BuildDwellingTest.java)
- Write: [IncreaseAvailableCreatures -> AvailableCreaturesChanged](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/write/changeavailablecreatures) | [test](src/test/java/com/dddheroes/heroesofddd/creaturerecruitment/write/changeavailablecreatures/IncreaseAvailableCreaturesTest.java)
- Write: [RecruitCreature -> CreatureRecruited](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/write/recruitcreature) | [test](src/test/java/com/dddheroes/heroesofddd/creaturerecruitment/write/recruitcreature)
- Read: (DwellingBuilt, AvailableCreaturesChanged, CreatureRecruited) -> DwellingReadModel [projector](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/read/DwellingReadModelProjector.java)
  - GetDwellingById: [query](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/read/getdwellingbyid/GetDwellingByIdQueryHandler.java) | [test](src/test/java/com/dddheroes/heroesofddd/creaturerecruitment/read/getdwellingbyid/GetDwellingByIdTest.java)
  - GetAllDwellings: [query](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/read/getalldwellings/GetAllDwellingsQueryHandler.java)
- Automation: [WhenCreatureRecruitedThenAddToArmy](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/automation/WhenCreatureRecruitedThenAddToArmyProcessor.java) | [test](src/test/java/com/dddheroes/heroesofddd/creaturerecruitment/automation/WhenCreatureRecruitedThenAddToArmyTest.java)

Aggregates:
- [Dwelling](src/main/java/com/dddheroes/heroesofddd/creaturerecruitment/write/Dwelling.java)

### 🧙 Astrologers

![EventModeling_Module_Astrologers.png](docs/images/EventModeling_Module_Astrologers.png)

Slices:
- Write: [ProclaimWeekSymbol -> WeekSymbolProclaimed](src/main/java/com/dddheroes/heroesofddd/astrologers/write/proclaimweeksymbol) | [test](src/test/java/com/dddheroes/heroesofddd/astrologers/write/proclaimweeksymbol/ProclaimWeekSymbolTest.java)
- Automation: [DayStarted(where day==1) -> ProclaimWeekSymbol](src/main/java/com/dddheroes/heroesofddd/astrologers/automation/whenweekstartedthenproclaimweeksymbol/WhenWeekStartedThenProclaimWeekSymbolProcessor.java) | [test](src/test/java/com/dddheroes/heroesofddd/astrologers/automation/whenweekstartedthenproclaimweeksymbol/WhenWeekStartedThenProclaimWeekSymbolTest.java)
- Automation: [(WeekSymbolProclaimed, all game dwellings derived from DwellingBuilt events) -> IncreaseAvailableCreatures for each dwelling in the game where creature == symbol](src/main/java/com/dddheroes/heroesofddd/astrologers/automation/whenweeksymbolproclaimedthenincreasedwellingavailablecreatures/WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor.java) | [test](src/test/java/com/dddheroes/heroesofddd/astrologers/automation/whenweeksymbolproclaimedthenincreasedwellingavailablecreatures/WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest.java)

Aggregates:
- [Astrologers](src/main/java/com/dddheroes/heroesofddd/astrologers/write/Astrologers.java)

### 📅 Calendar

![EventModeling_Module_Calendar.png](docs/images/EventModeling_Module_CalendarSlices.png)

Slices:
- Write: [StartDay -> DayStarted](src/main/java/com/dddheroes/heroesofddd/calendar/write/startday) | [test](src/test/java/com/dddheroes/heroesofddd/calendar/write/startday/StartDayTest.java)
- Write: [FinishDay -> DayFinished](src/main/java/com/dddheroes/heroesofddd/calendar/write/finishday) | [test](src/test/java/com/dddheroes/heroesofddd/calendar/write/finishday/FinishDayTest.java)
- Read: [DayStarted -> CurrentDateReadModel] -- todo

Aggregates:
- [Calendar](src/main/java/com/dddheroes/heroesofddd/calendar/write/Calendar.java)

### 🎖️ Armies

Slices:
- Write: [AddCreatureToArmy -> CreatureAddedToArmy](src/main/java/com/dddheroes/heroesofddd/armies/write/addcreature) | [test](src/test/java/com/dddheroes/heroesofddd/armies/write/addcreature/AddCreatureToArmyTest.java)
- Write: [RemoveCreatureFromArmy -> CreatureRemovedFromArmy](src/main/java/com/dddheroes/heroesofddd/armies/write/removecreature) | [test](src/test/java/com/dddheroes/heroesofddd/armies/write/removecreature/RemoveCreatureFromArmyTest.java)

Aggregates:
- [Army](src/main/java/com/dddheroes/heroesofddd/armies/write/Army.java)


## 🏛️ Screaming Architecture

The project follows a Screaming Architecture pattern organized around vertical slices that mirror Event Modeling concepts.

![ScreamingArchitecture](docs/images/ScreamingArchitectureEvents.png)

The package structure screams the capabilities of the system by making explicit: commands available to users, events that capture what happened, queries for retrieving information, business rules, and system automations.
This architecture makes it immediately obvious what the system can do, what rules govern those actions, and how different parts of the system interact through events.

Each module is structured into three distinct types of slices (packages `write`, `read`, `automation`) and there are events (package `events`) between them, which are a system backbone - a contract between all other parts:

### Write Slices
Contains commands that represent user intentions, defines business rules through aggregates, produces domain events, and enforces invariants (e.g., RecruitCreature command → CreatureRecruited event, with RecruitCreaturesNotExceedAvailableCreatures rule).

### Read Slices
Implements queries and read models optimized for specific use cases, with projectors that transform events into queryable state (e.g., GetDwellingById query → DwellingReadModel).

### Automation Slices
Processes events to trigger subsequent actions, implementing system policies and workflows that connect different modules (e.g., WhenCreatureRecruitedThenAddToArmyProcessor).

## 🧪 Testing
Tests using Real postgres Event Store, follows the approach:
- write slice: given(events) -> when(command) -> then(events)
- read slice: given(events) -> then(read model)
- automation: when(event, state?) -> then(command)

Tests are focused on observable behavior which implicitly covers the DDD Aggregates, so the domain model can be refactored without changes in tests.

### Example: write slice

![EventModeling_GWT_TestCase_CreatureRecruitment.png](docs/images/EventModeling_GWT_TestCase_CreatureRecruitment.png)

```java
@BeforeEach
void setUp() { // Axon Framework Test Fixture
    fixture = new AggregateTestFixture<>(Dwelling.class);
}
    
@Test
void givenDwellingWith2Creatures_WhenRecruit2Creatures_ThenRecruited() {
    // given
    var givenEvents = List.of(
            dwellingBuilt(),
            availableCreaturesChanged(2)
    );

    // when
    var whenCommand = recruitCreature(2);

    // then
    var thenEvent = creatureRecruited(2);
    fixture.given(givenEvents)
           .when(whenCommand)
           .expectEvents(thenEvent);
}
```


-------

### 💼 Hire me

If you'd like to hire me for Domain-Driven Design and/or Event Sourcing projects I'm available to work with:
Kotlin, Java, C# .NET, Ruby and JavaScript/TypeScript (Node.js or React).
Please reach me out on LinkedIn [linkedin.com/in/mateusznakodach/](https://www.linkedin.com/in/mateusznakodach/).
