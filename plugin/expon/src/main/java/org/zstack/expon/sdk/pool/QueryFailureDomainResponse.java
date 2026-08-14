package org.zstack.expon.sdk.pool;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryFailureDomainResponse extends ExponQueryResponse {
    private List<FailureDomainModule> failureDomains;

    public List<FailureDomainModule> getFailureDomains() {
        return failureDomains;
    }

    public void setFailureDomains(List<FailureDomainModule> failureDomains) {
        this.failureDomains = failureDomains;
    }
}
