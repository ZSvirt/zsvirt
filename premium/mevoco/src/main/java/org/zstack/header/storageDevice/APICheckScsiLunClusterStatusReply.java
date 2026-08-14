package org.zstack.header.storageDevice;

import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Create by weiwang at 2018/10/26
 */
@RestResponse(allTo = "inventory")
public class APICheckScsiLunClusterStatusReply extends APIReply {
    private ScsiLunClusterStatusInventory inventory;

    public ScsiLunClusterStatusInventory getInventory() {
        return inventory;
    }

    public void setInventory(ScsiLunClusterStatusInventory inventory) {
        this.inventory = inventory;
    }
    
    public static APICheckScsiLunClusterStatusReply __example__() {
        APICheckScsiLunClusterStatusReply reply = new APICheckScsiLunClusterStatusReply();
        ScsiLunClusterStatusInventory inv = new ScsiLunClusterStatusInventory();

        HostInventory hi1 = new HostInventory ();
        hi1.setAvailableCpuCapacity(2L);
        hi1.setAvailableMemoryCapacity(4L);
        hi1.setClusterUuid(uuid());
        hi1.setManagementIp("192.168.0.1");
        hi1.setName("example");
        hi1.setState(HostState.Enabled.toString());
        hi1.setStatus(HostStatus.Connected.toString());
        hi1.setClusterUuid(uuid());
        hi1.setZoneUuid(uuid());
        hi1.setUuid(uuid());
        hi1.setTotalCpuCapacity(4L);
        hi1.setTotalMemoryCapacity(4L);
        hi1.setHypervisorType("KVM");
        hi1.setDescription("example");

        HostInventory hi2 = new HostInventory ();
        hi2.setAvailableCpuCapacity(2L);
        hi2.setAvailableMemoryCapacity(4L);
        hi2.setClusterUuid(uuid());
        hi2.setManagementIp("192.168.0.2");
        hi2.setName("example");
        hi2.setState(HostState.Enabled.toString());
        hi2.setStatus(HostStatus.Connected.toString());
        hi2.setClusterUuid(uuid());
        hi2.setZoneUuid(uuid());
        hi2.setUuid(uuid());
        hi2.setTotalCpuCapacity(4L);
        hi2.setTotalMemoryCapacity(4L);
        hi2.setHypervisorType("KVM");
        hi2.setDescription("example");

        inv.setAttachedHosts(Arrays.asList(hi1));
        inv.setUnattachedHosts(Arrays.asList(hi2));
        inv.setAllHostsAttached(inv.getUnattachedHosts() == null || inv.getUnattachedHosts().size() == 0);

        reply.setInventory(inv);
        return reply;
    }
}
