package com.ncslzh.projects.criteriaengine.criterion;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MinCompletedOrdersCountCriterion<
        T extends MinCompletedOrdersCountCriterion.RequiredInput,
        U extends MinCompletedOrdersCountCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMinCompletedOrdersCount());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getCompletedOrdersCount()), "CompletedOrdersCount is null");
        return input.getCompletedOrdersCount() >= config.getMinCompletedOrdersCount();
    }

    public interface RequiredInput {
        Integer getCompletedOrdersCount();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        Integer getMinCompletedOrdersCount();
    }
}
