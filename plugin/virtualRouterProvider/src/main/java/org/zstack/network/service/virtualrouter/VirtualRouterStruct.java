package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmGlobalProperty;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.lb.LoadBalancerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 8/11/2015.
 */
public class VirtualRouterStruct {
    private L3NetworkInventory l3Network;
    private VirtualRouterOfferingValidator offeringValidator;
    private VirtualRouterVmSelector virtualRouterVmSelector;
    private VirtualRouterOfferingSelector virtualRouterOfferingSelector;
    private String loadBalancerUuid;
    private List<String> inherentSystemTags = new ArrayList<>();
    private List<String> nonInherentSystemTags = new ArrayList<>();
    private boolean notGatewayForGuestL3Network;
    private String providerType = VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    private String applianceVmType = VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE;
    private int applianceVmAgentPort = ApplianceVmGlobalProperty.AGENT_PORT;

    public VirtualRouterStruct() {
    }

    public VirtualRouterStruct(L3NetworkInventory l3) {
        this.l3Network = l3;
        if (l3 != null) {
            /*flat network*/
            if (l3.getNetworkServiceTypes().contains(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)) {
                this.setNotGatewayForGuestL3Network(true);
            }
        }
    }

    public int getApplianceVmAgentPort() {
        return applianceVmAgentPort;
    }

    public void setApplianceVmAgentPort(int applianceVmAgentPort) {
        this.applianceVmAgentPort = applianceVmAgentPort;
    }

    public VirtualRouterOfferingSelector getVirtualRouterOfferingSelector() {
        return virtualRouterOfferingSelector;
    }

    public void setVirtualRouterOfferingSelector(VirtualRouterOfferingSelector virtualRouterOfferingSelector) {
        this.virtualRouterOfferingSelector = virtualRouterOfferingSelector;
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

    public VirtualRouterOfferingValidator getOfferingValidator() {
        return offeringValidator;
    }

    public void setOfferingValidator(VirtualRouterOfferingValidator offeringValidator) {
        this.offeringValidator = offeringValidator;
    }

    public VirtualRouterVmSelector getVirtualRouterVmSelector() {
        return virtualRouterVmSelector;
    }

    public void setVirtualRouterVmSelector(VirtualRouterVmSelector virtualRouterVmSelector) {
        this.virtualRouterVmSelector = virtualRouterVmSelector;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
}
