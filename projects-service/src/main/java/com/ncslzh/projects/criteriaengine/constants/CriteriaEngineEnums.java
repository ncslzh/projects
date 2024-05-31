package com.ncslzh.projects.criteriaengine.constants;

import java.util.List;
import java.util.function.Function;

import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import com.ncslzh.projects.criteriaengine.dto.CriteriaEvaluationOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class CriteriaEngineEnums {

    /** Execution result of a {@link Criteria} depending on */
    public enum CriteriaExecutionResult {
        PASSED,
        FAILED;
    }

    /**
     * Strategies that determine if a {@link Criteria} should {@link
     * CriterionExecutionResult#PASSED} or {@link CriterionExecutionResult#FAILED}.
     *
     * <p> This should be used carefully together with {@link CriterionExecutionTerminationStrategy} to ensure that there are
     * no overlapping conflict.
     */
    @AllArgsConstructor
    public enum CriteriaExecutionResultStrategy {
        FAIL_IF_ANY_FAIL(o -> o.hasFailure() ? CriteriaExecutionResult.FAILED : CriteriaExecutionResult.PASSED),
        FAIL_IF_ALL_FAIL(o -> o.hasAllFailed() ? CriteriaExecutionResult.FAILED : CriteriaExecutionResult.PASSED),
        FAIL_IF_ANY_SKIP(o -> o.hasSkipped() ? CriteriaExecutionResult.FAILED : CriteriaExecutionResult.PASSED),
        FAIL_IF_ALL_SKIP(o -> o.hasAllSkipped() ? CriteriaExecutionResult.FAILED : CriteriaExecutionResult.PASSED),
        FAIL_IF_ANY_FAIL_OR_SKIP(o -> (o.hasSkipped() || o.hasFailure()) ? CriteriaExecutionResult.FAILED : CriteriaExecutionResult.PASSED),

        ALWAYS_PASS(o -> CriteriaExecutionResult.PASSED),
        PASS_ONLY_IF_ALL_PASS(o -> (!o.hasFailure() && !o.hasSkipped()) ? CriteriaExecutionResult.PASSED : CriteriaExecutionResult.FAILED)

        ;

        private Function<CriteriaEvaluationOutput, CriteriaExecutionResult> executionResultProvider;

        public CriteriaExecutionResult getExecutionResult(CriteriaEvaluationOutput executionOutput) {
            return executionResultProvider.apply(executionOutput);
        }
    }

    /**
     * Execution result of a {@link Criterion} that also depends on the {@link
     * CriterionExecutionTerminationStrategy} of a {@link Criteria}
     */
    @AllArgsConstructor
    @Getter
    public enum CriterionExecutionResult {
        PASSED(Criterion::getPassedMessage),
        SKIPPED(Criterion::getSkippedMessage),
        FAILED(Criterion::getFailedMessage);

        private final Function<Criterion<?, ?>, String> executionMessageProvider;
    }

    /** Strategies that determine if a {@link CriterionChain} should terminate early */
    @AllArgsConstructor
    public enum CriterionExecutionTerminationStrategy {
        CONTINUE_ALWAYS(List.of()),
        TERMINATE_IF_FAILED(List.of(CriterionExecutionResult.FAILED)),
        TERMINATE_IF_SKIPPED_OR_FAILED(List.of(CriterionExecutionResult.SKIPPED, CriterionExecutionResult.FAILED)),
        TERMINATE_IF_SKIPPED(List.of(CriterionExecutionResult.SKIPPED)),
        TERMINATE_IF_PASSED(List.of(CriterionExecutionResult.PASSED));

        private final List<CriterionExecutionResult> terminateUpon;

        public boolean shouldTerminate(CriterionExecutionResult status) {
            return terminateUpon.contains(status);
        }
    }
}
