## ▶︎ RESUME HERE
- next: drain queue — item 19 (query-handler/GetAllDwellingsQueryHandler)
- recipe: query-handler
- source: com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler
- tree: clean
- awaiting-caller: no

## Selection arguments (frozen frame)
framework=axoniq configuration=spring mode=project execution=inline

## OpenRewrite
status: success
ts: 2026-05-15T01:28:30+02:00
note: 78 files changed, committed 0fad52d

## Pinned decisions

## Queue
| # | recipe | source | status | last-commit | notes |
|---|--------|--------|--------|-------------|-------|
| 1 | aggregate | com.dddheroes.heroesofddd.armies.write.Army | done | | RemoveCreatureFromArmyTest: AggregateNotFoundException→DomainRule.ViolatedException; 8/8 tests green |
| 2 | aggregate | com.dddheroes.heroesofddd.astrologers.write.Astrologers | done | | Already AF5 shape post-OpenRewrite; 3/3 tests green |
| 3 | aggregate | com.dddheroes.heroesofddd.calendar.write.Calendar | done | | DayFinished: added @Event; FinishDayTest: exception→DomainRule.ViolatedException; 8/8 tests green |
| 4 | aggregate | com.dddheroes.heroesofddd.creaturerecruitment.write.Dwelling | done | | Exception flips: NullPointerException/DomainRule on empty entity; snapshot bean removed; 15/15 tests green |
| 5 | aggregate | com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool | done | | Already AF5 shape; 5/5 tests green |
| 6 | event-processor | com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WhenWeekStartedThenProclaimWeekSymbolProcessor | done | | Added @SequencingPolicy(MetadataSequencingPolicy, GAME_ID_KEY); removed YAML sequencing-policy; tests use @SpringBootTest not AggregateTestFixture |
| 7 | event-processor | com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor | done | | CommandGateway field → CommandDispatcher param; forEach→allOf async pattern; @SequencingPolicy added; orphaned Automation_WhenWeekSymbol*_Processor YAML key removed |
| 8 | event-processor | com.dddheroes.heroesofddd.creaturerecruitment.automation.WhenCreatureRecruitedThenAddToArmyProcessor | done | | @SequencingPolicy added; try/catch→exceptionallyCompose for compensation; removed YAML sequencing-policy |
| 9 | event-processor | com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelProjector | done | | @SequencingPolicy(MetadataSequencingPolicy, GAME_ID_KEY) added; already AF5-shaped post-OpenRewrite |
| 10 | event-processor | com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler | done | | @SequencingPolicy added; YAML Read_GetAllDwellings_QueryCache.sequencing-policy removed; dual-role event+query handler |
| 11 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingMcp | done | | .send().resultAs(Void.class) before .thenApply chain |
| 12 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingRestApi | done | | .send().resultAs(Void.class) |
| 13 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesMcp | done | | .send().resultAs(Void.class) before .thenApply chain |
| 14 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesRestApi | done | | .send().resultAs(Void.class) |
| 15 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureMcp | done | | .send().resultAs(Void.class) before .thenApply chain |
| 16 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureRestApi | done | | .send().resultAs(Void.class) |
| 17 | query-gateway | com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsMcp | done | | .get() → .get(5, TimeUnit.SECONDS) for timeout |
| 18 | query-gateway | com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdRestApi | done | | Already AF5-shaped; no changes |
| 19 | query-handler | com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler | pending | | |
| 20 | query-handler | com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdQueryHandler | pending | | |
| 21 | interceptors | com.dddheroes.heroesofddd.resourcespool.write.withdraw.PaidCommandInterceptor | pending | | |
| 22 | event-store | com.dddheroes.heroesofddd.HeroesOfDDDApplication | pending | | |

## Caller decisions log
