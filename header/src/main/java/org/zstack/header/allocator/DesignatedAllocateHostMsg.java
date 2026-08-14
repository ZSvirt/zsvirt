package org.zstack.header.allocator;

import org.zstack.header.vm.VmInstanceMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DesignatedAllocateHostMsg extends AllocateHostMsg implements VmInstanceMessage {
    private String zoneUuid;
    /**
     * OnlyAllowedClusters.
     * The final selected host must be in these clusters. (if clusterUuids is not empty)
     */
    private List<String> clusterUuids;
    private String hostUuid;

    public List<String> getClusterUuids() {
        return clusterUuids == null ? Collections.emptyList() : clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        if (clusterUuids != null) {
            this.clusterUuids = new ArrayList<>(clusterUuids.size());
            this.clusterUuids.addAll(clusterUuids);
        }
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        if (clusterUuid != null) {
            this.clusterUuids = Collections.singletonList(clusterUuid);
        }
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getVmInstance().getUuid();
    }
}
