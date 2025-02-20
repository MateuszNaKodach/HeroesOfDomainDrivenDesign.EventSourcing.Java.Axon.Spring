package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

public record RecruitCreaturesNotExceedAvailableCreatures(
        CreatureId dwellingCreatureId,
        Amount availableCreatures,
        CreatureId recruitCreatureId,
        Amount recruit
) implements DomainRule {

    @Override
    public boolean isViolated() {
        return !dwellingCreatureId.equals(recruitCreatureId) || recruit.compareTo(availableCreatures) > 0;
    }

    @Override
    public String message() {
        return "Recruit creatures not exceed available creatures";
    }
}
