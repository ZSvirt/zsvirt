package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetIscsiTargetBoundUssResponse extends ExponResponse {
    private List<IscsiBoundUssGatewayRefModule> nodes;

    public void setNodes(List<IscsiBoundUssGatewayRefModule> nodes) {
        this.nodes = nodes;
    }

    public List<IscsiBoundUssGatewayRefModule> getNodes() {
        return nodes;
    }
}
