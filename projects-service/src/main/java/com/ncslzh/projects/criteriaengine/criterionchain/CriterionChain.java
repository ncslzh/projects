package com.ncslzh.projects.criteriaengine.criterionchain;

import java.util.LinkedList;
import java.util.List;

import com.ncslzh.projects.criteriaengine.constants.CriteriaEngineEnums;
import com.ncslzh.projects.criteriaengine.criteria.Criteria;
import com.ncslzh.projects.criteriaengine.criterion.Criterion;
import com.ncslzh.projects.criteriaengine.dto.CriteriaEvaluationOutput;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CriterionChain<T, U extends Criteria.CriteriaConfig> {

    private final List<Criterion<T, U>> criteria;
    private final CriteriaEngineEnums.CriterionExecutionTerminationStrategy terminationStrategy;

    public CriterionChain() {
        criteria = new LinkedList<>();
        terminationStrategy = CriteriaEngineEnums.CriterionExecutionTerminationStrategy.TERMINATE_IF_FAILED;
    }

    public CriterionChain(CriteriaEngineEnums.CriterionExecutionTerminationStrategy terminationStrategy) {
        criteria = new LinkedList<>();
        this.terminationStrategy = terminationStrategy;
    }

    public void addCriterion(Criterion<T, U> criterion) {
        criteria.add(criterion);
    }

    public CriteriaEvaluationOutput checkCriteria(@NonNull T input, @NonNull U config) {

        CriteriaEvaluationOutput output = new CriteriaEvaluationOutput();

        for (Criterion<T, U> criterion : criteria) {
            CriteriaEngineEnums.CriterionExecutionResult executionResult = criterion.matchesCriterion(input, config);
            boolean isEarlyTermination = terminationStrategy.shouldTerminate(executionResult);

            output.updateStats(executionResult, isEarlyTermination);
            output
                    .getExecutionInfo()
                    .add(buildExecutionInfo(criterion, executionResult, isEarlyTermination));

            if (isEarlyTermination) {
                return output;
            }
        }

        return output;
    }

    private CriteriaEvaluationOutput.ExecutionInfo buildExecutionInfo(
            Criterion<T, U> criterion,
            CriteriaEngineEnums.CriterionExecutionResult executionResult,
            boolean isEarlyTermination) {

        return CriteriaEvaluationOutput.ExecutionInfo.builder()
                .className(criterion.getClass().getSimpleName())
                .executionResult(executionResult)
                .executionMessage(executionResult.getExecutionMessageProvider().apply(criterion))
                .earlyTerminated(isEarlyTermination)
                .build();
    }

    public static class Builder<T, U extends Criteria.CriteriaConfig> {
        private final CriterionChain<T, U> criteria;

        public Builder() {
            criteria = new CriterionChain<>();
        }

        public CriterionChain.Builder<T, U> addCriterion(Criterion<T, U> criterion) {
            criteria.addCriterion(criterion);
            return this;
        }

        public CriterionChain<T, U> build() {
            return criteria;
        }
    }
}
