package org.zstack.expon.sdk.vhost;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryVhostControllerResponse extends ExponQueryResponse {
    private List<VhostControllerModule> vhosts;

    public List<VhostControllerModule> getVhosts() {
        return vhosts;
    }

    public void setVhosts(List<VhostControllerModule> vhosts) {
        this.vhosts = vhosts;
    }
}
