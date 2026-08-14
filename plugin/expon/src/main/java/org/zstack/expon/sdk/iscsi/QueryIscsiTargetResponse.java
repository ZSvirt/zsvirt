package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryIscsiTargetResponse extends ExponQueryResponse {
    private List<IscsiModule> gateways;

    public List<IscsiModule> getGateways() {
        return gateways;
    }

    public void setGateways(List<IscsiModule> gateways) {
        this.gateways = gateways;
    }
}
