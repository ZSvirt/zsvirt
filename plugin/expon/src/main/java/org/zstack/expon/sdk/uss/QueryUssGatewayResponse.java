package org.zstack.expon.sdk.uss;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryUssGatewayResponse extends ExponQueryResponse {
    private List<UssGatewayModule> ussGateways;

    public void setUssGateways(List<UssGatewayModule> ussGateways) {
        this.ussGateways = ussGateways;
    }

    public List<UssGatewayModule> getUssGateways() {
        return ussGateways;
    }
}
