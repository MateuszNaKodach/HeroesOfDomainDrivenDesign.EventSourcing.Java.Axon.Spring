## ▶︎ RESUME HERE
- next: drain queue — item 1 (aggregate/Army)
- recipe: aggregate
- source: com.dddheroes.heroesofddd.armies.write.Army
- verify: axon4to5-isolatedtest target-name=Army
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
| 1 | aggregate | com.dddheroes.heroesofddd.armies.write.Army | pending | | |
| 2 | aggregate | com.dddheroes.heroesofddd.astrologers.write.Astrologers | pending | | |
| 3 | aggregate | com.dddheroes.heroesofddd.calendar.write.Calendar | pending | | |
| 4 | aggregate | com.dddheroes.heroesofddd.creaturerecruitment.write.Dwelling | pending | | |
| 5 | aggregate | com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool | pending | | |
| 6 | event-processor | com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WhenWeekStartedThenProclaimWeekSymbolProcessor | pending | | |
| 7 | event-processor | com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor | pending | | |
| 8 | event-processor | com.dddheroes.heroesofddd.creaturerecruitment.automation.WhenCreatureRecruitedThenAddToArmyProcessor | pending | | |
| 9 | event-processor | com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelProjector | pending | | |
| 10 | event-processor | com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler | pending | | |
| 11 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingMcp | pending | | |
| 12 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwellingRestApi | pending | | |
| 13 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesMcp | pending | | |
| 14 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreaturesRestApi | pending | | |
| 15 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureMcp | pending | | |
| 16 | command-gateway | com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreatureRestApi | pending | | |
| 17 | query-gateway | com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsMcp | pending | | |
| 18 | query-gateway | com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdRestApi | pending | | |
| 19 | query-handler | com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellingsQueryHandler | pending | | |
| 20 | query-handler | com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid.GetDwellingByIdQueryHandler | pending | | |
| 21 | interceptors | com.dddheroes.heroesofddd.resourcespool.write.withdraw.PaidCommandInterceptor | pending | | |
| 22 | event-store | com.dddheroes.heroesofddd.HeroesOfDDDApplication | pending | | |

## Caller decisions log
