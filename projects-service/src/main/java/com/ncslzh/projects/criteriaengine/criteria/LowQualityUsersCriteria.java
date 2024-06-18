package com.ncslzh.projects.criteriaengine.criteria;


import com.ncslzh.projects.placeholders.ConfigService;
import com.ncslzh.projects.criteriaengine.constants.CriteriaEngineEnums;
import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import com.ncslzh.projects.criteriaengine.criterion.MaxLiableOrderDisputesCountCriterion;
import com.ncslzh.projects.criteriaengine.criterion.MinAccountAgeDaysCriterion;
import com.ncslzh.projects.criteriaengine.criterion.MinBuyOrdersTotalValueSixMonthsCriterion;
import com.ncslzh.projects.criteriaengine.criterion.MinCompletedOrdersCountCriterion;
import com.ncslzh.projects.criteriaengine.criterion.MinCompletionRateCriterion;
import com.ncslzh.projects.criteriaengine.criterion.MinSellOrdersTotalValueSixMonthsCriterion;
import com.ncslzh.projects.criteriaengine.criterion.MinUniqueTradePartnersCountCriterion;
import com.ncslzh.projects.criteriaengine.criterionchain.CriterionChain;
import com.ncslzh.projects.criteriaengine.dto.CriteriaEvaluationOutput;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Criteria to check if a given user is considered as low quality or not. <a
 * href="https://okcoin.atlassian.net/browse/PMP-7733">JIRA Link</a>
 */
@Component
@Slf4j
public class LowQualityUsersCriteria
        implements Criteria<
        LowQualityUsersCriteria.CriteriaInput, LowQualityUsersCriteria.CriteriaConfig> {

    @Resource
    private ConfigService configService;

    @Resource
    private MinAccountAgeDaysCriterion<CriteriaInput, CriteriaConfig> minAccountAgeDaysCriterion;

    @Resource
    private MinCompletedOrdersCountCriterion<CriteriaInput, CriteriaConfig>
            minCompletedOrdersCountCriterion;

    @Resource
    private MinUniqueTradePartnersCountCriterion<CriteriaInput, CriteriaConfig>
            minUniqueTradePartnersCountCriterion;

    @Resource
    private MinCompletionRateCriterion<CriteriaInput, CriteriaConfig>
            minCompletionRateCriterion;

    @Resource
    private MinBuyOrdersTotalValueSixMonthsCriterion<CriteriaInput, CriteriaConfig>
            minBuyOrdersTotalValueSixMonthsCriterion;

    @Resource
    private MinSellOrdersTotalValueSixMonthsCriterion<CriteriaInput, CriteriaConfig>
            minSellOrdersTotalValueSixMonthsCriterion;

    @Resource
    private MaxLiableOrderDisputesCountCriterion<CriteriaInput, CriteriaConfig>
            maxLiableOrderDisputesCountCriterion;

    private List<Criterion<CriteriaInput, CriteriaConfig>> criteria;
    private CriterionChain<CriteriaInput, CriteriaConfig> criterionChain;

    private static final CriteriaConfig defaultCriteria = new CriteriaConfig();

    private static final CriteriaEngineEnums.CriteriaExecutionResultStrategy criteriaResultStrategy =
            CriteriaEngineEnums.CriteriaExecutionResultStrategy.FAIL_IF_ANY_FAIL;

    @PostConstruct
    public void init() {
        criteria =
                List.of(
                        minAccountAgeDaysCriterion,
                        minCompletedOrdersCountCriterion,
                        minUniqueTradePartnersCountCriterion,
                        minCompletionRateCriterion,
                        minBuyOrdersTotalValueSixMonthsCriterion,
                        minSellOrdersTotalValueSixMonthsCriterion,
                        maxLiableOrderDisputesCountCriterion);

        criterionChain =
                new CriterionChain<>(
                        CriteriaEngineEnums.CriterionExecutionTerminationStrategy.TERMINATE_IF_FAILED);
        for (Criterion<CriteriaInput, CriteriaConfig> criterion : criteria) {
            criterionChain.addCriterion(criterion);
        }
    }

    @Override
    public CriteriaConfig getCriteriaConfig(@NonNull String configName) {
        return configService.of(configName, defaultCriteria, CriteriaConfig.class);
    }

    @Override
    public boolean checkCriteria(@NonNull CriteriaInput input, @NonNull CriteriaConfig config) {
        CriteriaEvaluationOutput output = criterionChain.checkCriteria(input, config);
        CriteriaEngineEnums.CriteriaExecutionResult executionResult = criteriaResultStrategy.getExecutionResult(output);

        if (config.isDebugLog()) {
            log.info(
                    "[LowQualityUsersCriteria] input={}, \nconfig={}, \ncriteriaExecutionStrategy={}, \nexecutionResult={}, \noutput={}",
                    input,
                    config,
                    criteriaResultStrategy,
                    executionResult,
                    output);
        }

        return CriteriaEngineEnums.CriteriaExecutionResult.PASSED.equals(executionResult);
    }

    @Data
    @NoArgsConstructor
    public static class CriteriaConfig
            implements Criteria.CriteriaConfig,
            MinAccountAgeDaysCriterion.RequiredConfig,
            MinCompletedOrdersCountCriterion.RequiredConfig,
            MinUniqueTradePartnersCountCriterion.RequiredConfig,
            MinCompletionRateCriterion.RequiredConfig,
            MinBuyOrdersTotalValueSixMonthsCriterion.RequiredConfig,
            MinSellOrdersTotalValueSixMonthsCriterion.RequiredConfig,
            MaxLiableOrderDisputesCountCriterion.RequiredConfig {

        private Integer minAccountAgeDays;
        private Integer minCompletedOrdersCount;
        private Integer minUniqueTradePartnersCount;
        private Double minCompletionRate;
        private BigDecimal minBuyOrdersTotalValueSixMonths;
        private BigDecimal minSellOrdersTotalValueSixMonths;
        private Integer maxLiableOrderDisputesCount;
        private Boolean isDebugLog;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class CriteriaInput
            implements MinAccountAgeDaysCriterion.RequiredInput,
            MinCompletedOrdersCountCriterion.RequiredInput,
            MinUniqueTradePartnersCountCriterion.RequiredInput,
            MinCompletionRateCriterion.RequiredInput,
            MinBuyOrdersTotalValueSixMonthsCriterion.RequiredInput,
            MinSellOrdersTotalValueSixMonthsCriterion.RequiredInput,
            MaxLiableOrderDisputesCountCriterion.RequiredInput {

        // Inputs must be provided, but criteria can be null
        // ======== Required Inputs
        @NonNull
        private Integer accountAgeDays;
        @NonNull
        private Integer completedOrdersCount;
        @NonNull
        private Integer uniqueTradePartnersCount;
        @NonNull
        private Double completionRate;
        @NonNull
        private BigDecimal buyOrdersTotalValueSixMonths;
        @NonNull
        private BigDecimal sellOrdersTotalValueSixMonths;
        @NonNull
        private Integer liableOrderDisputesCount;
        // ======== Required Inputs

        @NonNull
        private String legalCurrencySymbol;

        // ======== For logging purposes
        @NonNull
        private Long userId;
        @NonNull
        private Integer brokerId;
        @NonNull
        private Long publicAdId;
    }
}
