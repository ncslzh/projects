package com.ncslzh.projects.criteriaengine.criterion;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
@Slf4j
public class MinCompletionRateCriterion<
        T extends MinCompletionRateCriterion.RequiredInput,
        U extends MinCompletionRateCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMinCompletionRate());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getCompletionRate()), "CompletionRate is null");
        return input.getCompletionRate().compareTo(config.getMinCompletionRate()) >= 0;
    }


    public interface RequiredInput {
        Double getCompletionRate();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        Double getMinCompletionRate();
    }
}
