package org.zstack.expon.sdk.vhost;

import org.zstack.expon.sdk.ExponResponse;
import org.zstack.expon.sdk.uss.UssGatewayModule;

import java.util.List;

public class GetVhostControllerBoundUssResponse extends ExponResponse {
    private List<UssGatewayModule> uss;

    public List<UssGatewayModule> getUss() {
        return uss;
    }

    public void setUss(List<UssGatewayModule> uss) {
        this.uss = uss;
    }
}
