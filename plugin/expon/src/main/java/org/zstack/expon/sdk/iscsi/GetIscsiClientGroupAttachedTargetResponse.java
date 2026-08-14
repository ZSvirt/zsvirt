package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetIscsiClientGroupAttachedTargetResponse extends ExponResponse {
    private List<IscsiModule> gateways;

    public void setGateways(List<IscsiModule> gateways) {
        this.gateways = gateways;
    }

    public List<IscsiModule> getGateways() {
        return gateways;
    }
}
