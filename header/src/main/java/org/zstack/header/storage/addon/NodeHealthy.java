package org.zstack.header.storage.addon;

import org.zstack.header.volume.VolumeProtocol;

import java.util.HashMap;
import java.util.Map;

public class NodeHealthy {
    private Map<VolumeProtocol, StorageHealthy> healthy;

    public void setHealthy(Map<VolumeProtocol, StorageHealthy> healthy) {
        this.healthy = healthy;
    }

    public Map<VolumeProtocol, StorageHealthy> getHealthy() {
        return healthy;
    }

    public StorageHealthy getHealthy(VolumeProtocol protocol) {
        return healthy.get(protocol);
    }

    public void setHealthy(VolumeProtocol protocol, StorageHealthy healthy) {
        if (this.healthy == null) {
            this.healthy = new HashMap<>();
        }
        this.healthy.put(protocol, healthy);
    }
}
