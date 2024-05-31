package com.ncslzh.projects.criteriaengine.criterion;

import java.math.BigDecimal;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MinSellOrdersTotalValueSixMonthsCriterion<
        T extends MinSellOrdersTotalValueSixMonthsCriterion.RequiredInput,
        U extends MinSellOrdersTotalValueSixMonthsCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMinSellOrdersTotalValueSixMonths());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getSellOrdersTotalValueSixMonths()), "SellOrderTotalValue is null");
        return input
                .getSellOrdersTotalValueSixMonths()
                .compareTo(config.getMinSellOrdersTotalValueSixMonths())
                >= 0;
    }

    public interface RequiredInput {
        BigDecimal getSellOrdersTotalValueSixMonths();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        BigDecimal getMinSellOrdersTotalValueSixMonths();
    }
}
