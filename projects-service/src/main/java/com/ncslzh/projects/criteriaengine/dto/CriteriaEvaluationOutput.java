package com.ncslzh.projects.criteriaengine.dto;

import java.util.ArrayList;
import java.util.List;

import com.ncslzh.projects.criteriaengine.constants.CriteriaEngineEnums;
import lombok.*;

@Getter
@Setter
@ToString
public class CriteriaEvaluationOutput {

    public CriteriaEvaluationOutput() {
        executionInfo = new ArrayList<>();
        executionStats = new ExecutionStats();
    }

    private List<ExecutionInfo> executionInfo;
    private ExecutionStats executionStats;


    public boolean hasTerminatedEarly() {
        return executionStats.isTerminatedEarly();
    }

    public boolean hasFailure() {
        return executionStats.getFailedCount() > 0;
    }

    public boolean hasAllFailed() {
        return executionStats.getFailedCount() == executionInfo.size();
    }

    public boolean hasSkipped() {
        return executionStats.getSkippedCount() > 0;
    }

    public boolean hasAllSkipped() {
        return executionStats.getSkippedCount() == executionInfo.size();
    }

    public void updateStats(CriteriaEngineEnums.CriterionExecutionResult executionStatus, boolean isEarlyTermination) {
        executionStats.incrementStat(executionStatus);
        executionStats.setTerminatedEarly(isEarlyTermination);
    }

    @Getter
    @Builder
    @ToString
    public static class ExecutionInfo {
        private String className;
        private CriteriaEngineEnums.CriterionExecutionResult executionResult;
        private String executionMessage;
        private boolean earlyTerminated;

    }

    @NoArgsConstructor
    @Data
    public static class ExecutionStats {
        private int passedCount = 0;
        private int failedCount = 0;
        private int skippedCount = 0;
        private boolean terminatedEarly = false;

        public void incrementStat(CriteriaEngineEnums.CriterionExecutionResult result) {
            switch (result) {
                case PASSED -> passedCount++;
                case FAILED -> failedCount++;
                case SKIPPED -> skippedCount++;
            }
        }
    }
}
