package org.zstack.core.asyncbatch;

public interface WhileProcessor {
    default void beforeStart(long totalCount) {}

    default void afterDone(long completedCount) {}

    default void afterAllDone() {}
}
