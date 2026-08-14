package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetIscsiTargetServerResponse extends ExponResponse {
    List<IscsiSeverNode> nodes;

    public void setNodes(List<IscsiSeverNode> nodes) {
        this.nodes = nodes;
    }

    public List<IscsiSeverNode> getNodes() {
        return nodes;
    }
}
