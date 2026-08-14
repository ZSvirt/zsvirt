package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 8/11/2015.
 */
public class CreateVirtualRouterVmMsg extends NeedReplyMessage {
    private L3NetworkInventory l3Network;
    private VirtualRouterOfferingInventory offering;
    private List<String> inherentSystemTags = new ArrayList<>();
    private List<String> nonInherentSystemTags = new ArrayList<>();
    private boolean notGatewayForGuestL3Network;
    private String providerType;
    private String applianceVmType;
    private int applianceVmAgentPort;

    public int getApplianceVmAgentPort() {
        return applianceVmAgentPort;
    }

    public void setApplianceVmAgentPort(int applianceVmAgentPort) {
        this.applianceVmAgentPort = applianceVmAgentPort;
    }

    public String getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(String applianceVmType) {
        this.applianceVmType = applianceVmType;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public boolean isNotGatewayForGuestL3Network() {
        return notGatewayForGuestL3Network;
    }

    public void setNotGatewayForGuestL3Network(boolean notGatewayForGuestL3Network) {
        this.notGatewayForGuestL3Network = notGatewayForGuestL3Network;
    }

    public List<String> getNonInherentSystemTags() {
        return nonInherentSystemTags;
    }

    public void setNonInherentSystemTags(List<String> nonInherentSystemTags) {
        this.nonInherentSystemTags = nonInherentSystemTags;
    }

    public List<String> getInherentSystemTags() {
        return inherentSystemTags;
    }

    public void setInherentSystemTags(List<String> inherentSystemTags) {
        this.inherentSystemTags = inherentSystemTags;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }

    public VirtualRouterOfferingInventory getOffering() {
        return offering;
    }

    public void setOffering(VirtualRouterOfferingInventory offering) {
        this.offering = offering;
    }
}
