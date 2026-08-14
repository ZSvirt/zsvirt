package org.zstack.expon.sdk.pool;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetFailureDomainBlacklistResponse extends ExponResponse {
    private List<BlacklistModule> entries;
    private String poolId;
    private String bepoch;

    public List<BlacklistModule> getEntries() {
        return entries;
    }

    public void setEntries(List<BlacklistModule> entries) {
        this.entries = entries;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getBepoch() {
        return bepoch;
    }

    public void setBepoch(String bepoch) {
        this.bepoch = bepoch;
    }
}
