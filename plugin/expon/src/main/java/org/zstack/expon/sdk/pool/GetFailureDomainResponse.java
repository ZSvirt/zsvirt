package org.zstack.expon.sdk.pool;

import org.zstack.expon.sdk.ExponResponse;

public class GetFailureDomainResponse extends ExponResponse {
    private FailureDomainModule members;

    public FailureDomainModule getMembers() {
        return members;
    }

    public void setMembers(FailureDomainModule members) {
        this.members = members;
    }
}
