package com.ncslzh.projects.criteriaengine.criterion;

import java.math.BigDecimal;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MinBuyOrdersTotalValueSixMonthsCriterion<
        T extends MinBuyOrdersTotalValueSixMonthsCriterion.RequiredInput,
        U extends MinBuyOrdersTotalValueSixMonthsCriterion.RequiredConfig>
        implements Criterion<T, U> {

    @Override
    public boolean shouldCheckCriterion(@NonNull U config) {
        return Objects.nonNull(config.getMinBuyOrdersTotalValueSixMonths());
    }

    @Override
    public boolean matchesCriterionCore(@NonNull T input, @NonNull U config) {
        Preconditions.checkArgument(Objects.nonNull(input.getBuyOrdersTotalValueSixMonths()), "BuyOrderTotalValue is null");
        return input
                .getBuyOrdersTotalValueSixMonths()
                .compareTo(config.getMinBuyOrdersTotalValueSixMonths())
                >= 0;
    }

    public interface RequiredInput {
        BigDecimal getBuyOrdersTotalValueSixMonths();
    }

    public interface RequiredConfig extends Criteria.CriteriaConfig {
        BigDecimal getMinBuyOrdersTotalValueSixMonths();
    }
}
