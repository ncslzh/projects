package com.ncslzh.projects.criteriaengine.criterion;

import com.ncslzh.projects.criteriaengine.constants.CriteriaEngineEnums;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import lombok.NonNull;

/**
 * A criterion encapsulate a single business context validation.
 *
 * @param <T> Input data against which the criterion will be evaluated using
 * @param <U> Configured parameter-value that is used against the input
 */
public interface Criterion<T, U extends Criteria.CriteriaConfig> {

    /** Method for turning debug logs on/off. */
    default boolean isDebugLog(@NonNull U config) {
        return Boolean.TRUE.equals(config.getIsDebugLog());
    }

    /** Custom messages can be over-ridden by implementing classes */
    default String getFailedMessage() {
        return String.format("%-25s | Failed", this.getClass().getSimpleName());
    }

    default String getPassedMessage() {
        return String.format("%-25s | Passed", this.getClass().getSimpleName());
    }

    default String getSkippedMessage() {
        return String.format("%-25s | Skipped", this.getClass().getSimpleName());
    }

    /** Main method to be used when checking if a criterion passes or not. */
    default CriteriaEngineEnums.CriterionExecutionResult matchesCriterion(@NonNull T input, @NonNull U config) {

        if (!shouldCheckCriterion(config)) {
            return CriteriaEngineEnums.CriterionExecutionResult.SKIPPED;
        }

        return matchesCriterionCore(input, config)
                ? CriteriaEngineEnums.CriterionExecutionResult.PASSED
                : CriteriaEngineEnums.CriterionExecutionResult.FAILED;
    }

    /** Method for allowing a criterion to be skipped if needed */
    boolean shouldCheckCriterion(@NonNull U config);

    /** Actual evaluation logic function */
    boolean matchesCriterionCore(@NonNull T input, @NonNull U config);
}
