package org.zstack.header.identity.quota;

import org.zstack.utils.function.Function;

public class FunctionalSizeRequiredRequest<T> implements QuotaRequiredRequest {
    private String quotaName;
    private Function<Long, T> function;

    @Override
    public String getQuotaName() {
        return quotaName;
    }

    public void setQuotaName(String quotaName) {
        this.quotaName = quotaName;
    }

    public Function<Long, T> getFunction() {
        return function;
    }

    public void setFunction(Function<Long, T> function) {
        this.function = function;
    }
}
