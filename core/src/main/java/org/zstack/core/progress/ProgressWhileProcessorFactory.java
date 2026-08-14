package org.zstack.core.progress;

import org.zstack.core.asyncbatch.WhileProcessor;

public class ProgressWhileProcessorFactory {
    public WhileProcessor create(String content) {
        return new ProgressWhileProcessor(content);
    }
}
