package com.ncslzh.projects.criteriaengine.criterion;

import java.util.Objects;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MinAccountAgeDaysCriterion<
        T extends MinAccountAgeDaysCriterion.RequiredInput,
        U extends MinAccountAgeDaysCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMinAccountAgeDays());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getAccountAgeDays()), "AccountAgeDays is null");
        return input.getAccountAgeDays() >= config.getMinAccountAgeDays();
    }

    public interface RequiredInput {
        Integer getAccountAgeDays();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        Integer getMinAccountAgeDays();
    }
}
