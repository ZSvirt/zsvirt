package org.zstack.storage.device.nvme;

import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2022/8/10.
 */

@RestResponse(allTo = "inventories")
public class APIRefreshNvmeTargetEvent extends APIEvent {
    private List<NvmeTargetInventory> inventories;

    public List<NvmeTargetInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NvmeTargetInventory> inventories) {
        this.inventories = inventories;
    }

    public APIRefreshNvmeTargetEvent() {
    }

    public APIRefreshNvmeTargetEvent(String apiId) {
        super(apiId);
    }

    public static APIRefreshNvmeTargetEvent __example__() {
        APIRefreshNvmeTargetEvent evt = new APIRefreshNvmeTargetEvent();

        NvmeLunInventory nvmeLunInv11 = new NvmeLunInventory();
        nvmeLunInv11.setName("nvme-lun-36b083fe000daf018000022905ba35d8f");
        nvmeLunInv11.setNvmeTargetUuid(uuid());
        nvmeLunInv11.setUuid(uuid());
        nvmeLunInv11.setWwn("uuid.48daeab7-7f15-405e-8481-7152cb9b0aca");
        nvmeLunInv11.setType("disk");
        nvmeLunInv11.setSerial("3d87eca1686c1782");
        nvmeLunInv11.setSize(5497558138880L);
        nvmeLunInv11.setWwid("uuid.48daeab7-7f15-405e-8481-7152cb9b0aca");
        nvmeLunInv11.setPath("nvme-uuid.48daeab7-7f15-405e-8481-7152cb9b0aca ");

        NvmeTargetInventory nvmeTargetInv1 = new NvmeTargetInventory();
        nvmeTargetInv1.setUuid(uuid());
        nvmeTargetInv1.setState(StorageDeviceState.Enabled.toString());
        nvmeTargetInv1.setCreateDate(new Timestamp(DocUtils.date));
        nvmeTargetInv1.setLastOpDate(new Timestamp(DocUtils.date));
        nvmeTargetInv1.setNvmeLuns(Collections.singletonList(nvmeLunInv11));
        nvmeLunInv11.setNvmeTargetUuid(nvmeTargetInv1.getUuid());

        HostInventory hi = new HostInventory ();
        hi.setAvailableCpuCapacity(2L);
        hi.setAvailableMemoryCapacity(4L);
        hi.setClusterUuid(uuid());
        hi.setManagementIp("192.168.0.1");
        hi.setName("example");
        hi.setState(HostState.Enabled.toString());
        hi.setStatus(HostStatus.Connected.toString());
        hi.setClusterUuid(uuid());
        hi.setZoneUuid(uuid());
        hi.setUuid(uuid());
        hi.setTotalCpuCapacity(4L);
        hi.setTotalMemoryCapacity(4L);
        hi.setHypervisorType("KVM");
        hi.setDescription("example");

        NvmeLunHostRefInventory refInventory11 = new NvmeLunHostRefInventory();
        refInventory11.setId(1);
        refInventory11.setNvmeLunUuid(nvmeLunInv11.getUuid());
        refInventory11.setHostUuid(hi.getUuid());
        refInventory11.setCreateDate(new Timestamp(DocUtils.date));
        refInventory11.setLastOpDate(new Timestamp(DocUtils.date));

        nvmeLunInv11.setNvmeLunHostRefs(Collections.singletonList(refInventory11));

        evt.setInventories(Collections.singletonList(nvmeTargetInv1));
        return evt;
    }
}
