package org.zstack.header.identity.quota;

import org.zstack.header.message.Message;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * QuotaMessageHandler used for messages that need quota check
 * to define how to get message required values
 * @param <T>
 */
public class QuotaMessageHandler<T extends Message> {
    public Class<T> messageClass;

    private List<FixedSizeRequiredRequest> fixedSizeRequiredRequests = new ArrayList<>();
    private List<FunctionalSizeRequiredRequest<T>> functionalSizeRequiredRequests = new ArrayList<>();
    private List<Function<Boolean, T>> conditions = new ArrayList<>();

    public QuotaMessageHandler(Class<T> clazz) {
        this.messageClass = clazz;
    }

    /**
     * when message invoked, count 1 to the quota
     * @param quotaName required quota name
     * @return this
     */
    public QuotaMessageHandler<T> addCounterQuota(String quotaName) {
        fixedSizeRequiredRequests.add(new FixedSizeRequiredRequest(quotaName, 1L));
        return this;
    }

    /**
     * when message invoked, return a fixed required value
     * @param quotaName required quota name
     * @param value required value
     * @return this
     */
    public QuotaMessageHandler<T> addFixedRequiredSize(String quotaName, Long value) {
        FixedSizeRequiredRequest request = new FixedSizeRequiredRequest(quotaName, value);
        fixedSizeRequiredRequests.add(request);
        return this;
    }

    /**
     * when message invoked, apply function to get required value from message
     * @param quotaName required quota name
     * @param function function with message as input and Long as return to get
     *                 required size
     * @return this
     */
    public QuotaMessageHandler<T> addMessageRequiredQuotaHandler(String quotaName, Function<Long, T> function) {
        FunctionalSizeRequiredRequest<T> request = new FunctionalSizeRequiredRequest<>();
        request.setQuotaName(quotaName);
        request.setFunction(function);
        functionalSizeRequiredRequests.add(request);
        return this;
    }

    /**
     * when message invoked, apply condition check to decide if need to
     * skip this message's quota check
     * @param function function with message as input and Boolean as return to
     *                 tell whether we need skip this message's quota check
     * @return this
     */
    public QuotaMessageHandler<T> addCheckCondition(Function<Boolean, T> function) {
        conditions.add(function);
        return this;
    }

    public List<FixedSizeRequiredRequest> getFixedSizeRequiredRequests() {
        return fixedSizeRequiredRequests;
    }

    public void setFixedSizeRequiredRequests(List<FixedSizeRequiredRequest> fixedSizeRequiredRequests) {
        this.fixedSizeRequiredRequests = fixedSizeRequiredRequests;
    }

    public List<FunctionalSizeRequiredRequest<T>> getFunctionalSizeRequiredRequests() {
        return functionalSizeRequiredRequests;
    }

    public void setFunctionalSizeRequiredRequests(List<FunctionalSizeRequiredRequest<T>> functionalSizeRequiredRequests) {
        this.functionalSizeRequiredRequests = functionalSizeRequiredRequests;
    }

    public List<Function<Boolean, T>> getConditions() {
        return conditions;
    }

    public void setConditions(List<Function<Boolean, T>> conditions) {
        this.conditions = conditions;
    }
}
