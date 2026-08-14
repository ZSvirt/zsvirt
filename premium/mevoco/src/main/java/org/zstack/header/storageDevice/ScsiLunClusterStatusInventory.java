package org.zstack.header.storageDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;

import java.io.Serializable;
import java.util.List;

/**
 * Create by weiwang at 2018/10/26
 */
@PythonClassInventory
public class ScsiLunClusterStatusInventory implements Serializable {
    private List<HostInventory> attachedHosts;

    private List<HostInventory> unattachedHosts;

    private Boolean isAllHostsAttached;

    public ScsiLunClusterStatusInventory() {
    }

    public List<HostInventory> getAttachedHosts() {
        return attachedHosts;
    }

    public void setAttachedHosts(List<HostInventory> attachedHosts) {
        this.attachedHosts = attachedHosts;
    }

    public List<HostInventory> getUnattachedHosts() {
        return unattachedHosts;
    }

    public void setUnattachedHosts(List<HostInventory> unattachedHosts) {
        this.unattachedHosts = unattachedHosts;
    }

    public Boolean getAllHostsAttached() {
        return isAllHostsAttached;
    }

    public void setAllHostsAttached(Boolean allHostsAttached) {
        isAllHostsAttached = allHostsAttached;
    }
}
