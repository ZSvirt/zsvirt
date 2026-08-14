package org.zstack.core.progress;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TaskProgressReporter {
    private static final CLogger logger = Utils.getLogger(TaskProgressReporter.class);

    String content;
    Map<String, Object> opaque;
    long currentStep = 0L, totalStep = 100L;

    String apiId;
    long id = INVALID_ID;
    public static final long INVALID_ID = -1L;

    public TaskProgressReporter withContent(String content) {
        this.content = content;
        return this;
    }

    public TaskProgressReporter withOpaque(String key, Object value) {
        if (this.opaque == null) {
            opaque = new HashMap<>();
        }
        this.opaque.put(key, value);
        return this;
    }

    public <T> TaskProgressReporter withOpaques(Map<String, T> map) {
        if (this.opaque == null) {
            opaque = new HashMap<>();
        }
        this.opaque.putAll(map);
        return this;
    }

    public TaskProgressReporter withCurrentStep(long currentStep) {
        this.currentStep = currentStep;
        return this;
    }

    public TaskProgressReporter withTotalStep(long totalStep) {
        this.totalStep = totalStep;
        return this;
    }

    public TaskProgressReporter withApiId(String apiId) {
        this.apiId = apiId;
        return this;
    }

    public TaskProgressReporter report() {
        if (this.apiId == null) {
            apiId = ActionProgressService.findApiId();
        }

        if (apiId == null) {
            logger.warn("apiId not found for task info " + toString());
            return this;
        }

        if (content == null) {
            throw new CloudRuntimeException("TaskProgressReporter.content cannot be null");
        }

        ActionProgressService.putToCache(this);
        return this;
    }

    @Override
    public String toString() {
        return Objects.toString(content);
    }
}
