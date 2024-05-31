package com.ncslzh.projects.criteriaengine.criterion;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MaxLiableOrderDisputesCountCriterion<
        T extends MaxLiableOrderDisputesCountCriterion.RequiredInput,
        U extends MaxLiableOrderDisputesCountCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMaxLiableOrderDisputesCount());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getLiableOrderDisputesCount()), "LiableOrderDisputes is null");
        return input.getLiableOrderDisputesCount() <= config.getMaxLiableOrderDisputesCount();
    }

    public interface RequiredInput {
        Integer getLiableOrderDisputesCount();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        Integer getMaxLiableOrderDisputesCount();
    }
}
