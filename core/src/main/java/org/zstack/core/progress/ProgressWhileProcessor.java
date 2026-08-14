package org.zstack.core.progress;

import org.zstack.core.asyncbatch.WhileProcessor;

public class ProgressWhileProcessor implements WhileProcessor {
    TaskProgressReporter reporter;
    long totalStep;

    public ProgressWhileProcessor(String content) {
        reporter = ActionProgressService.taskProgress()
                .withContent(content);
    }

    public void beforeStart(long totalCount) {
        totalStep = Math.max(totalCount, 1L);
        reporter.withCurrentStep(0)
                .withTotalStep(Math.max(totalCount, 1L))
                .report();
    }

    public void afterDone(long completedCount) {
        reporter.withCurrentStep(completedCount).report();
    }

    public void afterAllDone() {
        reporter.withCurrentStep(totalStep).report();
    }
}
