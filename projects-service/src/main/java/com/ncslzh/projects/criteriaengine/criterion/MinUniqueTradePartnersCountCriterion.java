package com.ncslzh.projects.criteriaengine.criterion;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MinUniqueTradePartnersCountCriterion<
        T extends MinUniqueTradePartnersCountCriterion.RequiredInput,
        U extends MinUniqueTradePartnersCountCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMinUniqueTradePartnersCount());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getUniqueTradePartnersCount()), "UniqueTradePartnersCount is null");
        return input.getUniqueTradePartnersCount() >= config.getMinUniqueTradePartnersCount();
    }

    public interface RequiredInput {
        Integer getUniqueTradePartnersCount();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        Integer getMinUniqueTradePartnersCount();
    }
}
