package org.zstack.network.service.virtualrouter;

import java.util.List;

public class VmVirtualRouterVmRelation {
    public static class VirtualRouterVmL3NetworkRelation {
        private String vrUuid;
        private String l3NetworkUuid;
        private List<String> networkServiceTypes;
        public String getVrUuid() {
            return vrUuid;
        }
        public void setVrUuid(String vrUuid) {
            this.vrUuid = vrUuid;
        }
        public String getL3NetworkUuid() {
            return l3NetworkUuid;
        }
        public void setL3NetworkUuid(String l3NetworkUuid) {
            this.l3NetworkUuid = l3NetworkUuid;
        }
        public List<String> getNetworkServiceTypes() {
            return networkServiceTypes;
        }
        public void setNetworkServiceTypes(List<String> networkServiceTypes) {
            this.networkServiceTypes = networkServiceTypes;
        }
    }
    
    private String vmInstanceUuid;
    private List<VirtualRouterVmL3NetworkRelation> virtualRouterVmsForL3Networks;
    public String getVmUuid() {
        return vmInstanceUuid;
    }
    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public List<VirtualRouterVmL3NetworkRelation> getVirtualRouterVmsForL3Networks() {
        return virtualRouterVmsForL3Networks;
    }
    public void setVirtualRouterVmsForL3Networks(List<VirtualRouterVmL3NetworkRelation> virtualRouterVmsForL3Networks) {
        this.virtualRouterVmsForL3Networks = virtualRouterVmsForL3Networks;
    }
}
