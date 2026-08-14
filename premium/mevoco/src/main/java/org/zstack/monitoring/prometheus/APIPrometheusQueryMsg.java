package org.zstack.monitoring.prometheus;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by xing5 on 2016/7/14.
 */
@Deprecated
public abstract class APIPrometheusQueryMsg extends APISyncCallMessage {
    protected boolean instant;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    protected Long startTime;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    protected Long endTime;
    protected String step;
    @APIParam
    protected String expression;
    protected String relativeTime;

    public String getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(String relativeTime) {
        this.relativeTime = relativeTime;
    }

    public boolean isInstant() {
        return instant;
    }

    public void setInstant(boolean instant) {
        this.instant = instant;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

}