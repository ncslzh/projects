package com.ncslzh.projects.criteriaengine.criteria;

import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import lombok.NonNull;

/**
 * Encapsulates a set of business-defined criteria that should be met
 *
 * <p>A {@code Criteria} is an aggregate of {@link Criterion}.
 *
 * <p>See {@code README.md} for how to add new {@code Criteria} and {@code Criterion}
 *
 * @param <T> Input data against which the criteria that will be evaluated
 * @param <U> Configured parameter-value for each {@link Criterion}
 */
public interface Criteria<T, U extends Criteria.CriteriaConfig> {

    /**
     * Gets the {@link U} configuration values for each {@link Criterion} for validation. If config
     * does not exist, returns a predefined default configuration
     */
    U getCriteriaConfig(String configName);

    /**
     * Given input T, checks if the input data {@link T} meets the set criteria defined in {@code
     * criteria} and using criteria config values {@link U} obtained ideally from {@link
     * this#getCriteriaConfig(String)}
     *
     * <p>Ideally, {@link U} config should be instantiated once in calling method and used for multiple {@code this#checkCriteria},
     * otherwise the cost of creating the same config for each call to {@code this#checkCriteria} is unecessarily expensive
     */
    boolean checkCriteria(@NonNull T input, U config);

    /**
     * Contains common non-business specific fields that can be used by all {@link Criteria}
     * <p>Implementing classes of the {@link Criteria} interface should also contain another business-specific
     * {@code CriteriaConfig} interface to encapsulate business-specific criteria/logic.
     *
     * <p>See {@link LowQualityUsersCriteria.CriteriaConfig} for an example
     */
    interface CriteriaConfig {
        Boolean getIsDebugLog();

        default boolean isDebugLog() {
            return Boolean.TRUE.equals(getIsDebugLog());
        }
    }
}
