package org.zstack.storage.device.nvme;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventory")
public class APIDetachNvmeServerFromClusterEvent extends APIEvent {
    private NvmeServerInventory inventory;

    public NvmeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(NvmeServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIDetachNvmeServerFromClusterEvent(String apiId) {
        super(apiId);
    }

    public APIDetachNvmeServerFromClusterEvent() {
        super(null);
    }

    public static APIDetachNvmeServerFromClusterEvent __example__() {
        APIDetachNvmeServerFromClusterEvent event = new APIDetachNvmeServerFromClusterEvent();

        NvmeLunInventory nvmeLunInventory = new NvmeLunInventory();
        nvmeLunInventory.setUuid(uuid());
        nvmeLunInventory.setWwn("0x6b083fe000daf018");
        nvmeLunInventory.setHctl("6:0:1:1");
        nvmeLunInventory.setModel("MD32xx");
        nvmeLunInventory.setVendor("DELL");
        nvmeLunInventory.setType("disk");
        nvmeLunInventory.setSerial("6b083fe000daf018000015505abbe00a");
        nvmeLunInventory.setSize(30003188203520l);
        nvmeLunInventory.setWwid("36b083fe000daf018000015505abbe00a");
        nvmeLunInventory.setPath("uuid.a5ab00e7-3bfe-4d3d-bc57-bb4032e04951");

        NvmeLunInventory nvmeLunInventory2 = new NvmeLunInventory();
        nvmeLunInventory2.setUuid(uuid());
        nvmeLunInventory2.setWwn("0x6b083fe000daf018");
        nvmeLunInventory2.setHctl("6:0:3:1");
        nvmeLunInventory2.setModel("MD32xx");
        nvmeLunInventory2.setVendor("DELL");
        nvmeLunInventory2.setType("disk");
        nvmeLunInventory2.setSerial("6b083fe000daf018000015505abbe00a");
        nvmeLunInventory2.setSize(30003188203520l);
        nvmeLunInventory2.setWwid("36b083fe000daf018000015505abbe00a");
        nvmeLunInventory2.setPath("uuid.a5ab00e7-3bfe-4d3d-bc57-bb4032e04952");

        NvmeTargetInventory nvmeTargetInventory = new NvmeTargetInventory();
        nvmeTargetInventory.setUuid(uuid());
        nvmeTargetInventory.setState(StorageDeviceState.Enabled.toString());
        nvmeTargetInventory.setNqn("nqn.2014-08.org.nvmexpress.example1");
        nvmeTargetInventory.setCreateDate(new Timestamp(DocUtils.date));
        nvmeTargetInventory.setLastOpDate(new Timestamp(DocUtils.date));
        nvmeTargetInventory.setNvmeLuns(asList(nvmeLunInventory));
        nvmeLunInventory.setNvmeTargetUuid(nvmeTargetInventory.getUuid());

        NvmeTargetInventory nvmeTargetInventory2 = new NvmeTargetInventory();
        nvmeTargetInventory2.setUuid(uuid());
        nvmeTargetInventory2.setState(StorageDeviceState.Enabled.toString());
        nvmeTargetInventory2.setNqn("nqn.2014-08.org.nvmexpress.example2");
        nvmeTargetInventory2.setCreateDate(new Timestamp(DocUtils.date));
        nvmeTargetInventory2.setLastOpDate(new Timestamp(DocUtils.date));
        nvmeTargetInventory2.setNvmeLuns(asList(nvmeLunInventory2));
        nvmeLunInventory2.setNvmeTargetUuid(nvmeTargetInventory2.getUuid());

        NvmeServerInventory nvmeServerInventory = new NvmeServerInventory();
        nvmeServerInventory.setUuid(uuid());
        nvmeServerInventory.setState(StorageDeviceState.Enabled.toString());
        nvmeServerInventory.setName("nvme-server-10.0.0.201");
        nvmeServerInventory.setIp("10.0.0.201");
        nvmeServerInventory.setPort(4420);
        nvmeServerInventory.setCreateDate(new Timestamp(DocUtils.date));
        nvmeServerInventory.setLastOpDate(new Timestamp(DocUtils.date));
        nvmeServerInventory.setNvmeTargets(asList(nvmeTargetInventory));

        ClusterInventory cluster = new ClusterInventory();
        cluster.setHypervisorType("KVM");
        cluster.setName("cluster1");
        cluster.setDescription("test");
        cluster.setState(ClusterState.Enabled.toString());
        cluster.setZoneUuid(uuid());
        cluster.setUuid(uuid());
        cluster.setType("zstack");
        cluster.setCreateDate(new Timestamp(DocUtils.date));
        cluster.setLastOpDate(new Timestamp(DocUtils.date));

        NvmeServerClusterRefInventory refInventory = new NvmeServerClusterRefInventory();
        refInventory.setId(1);
        refInventory.setNvmeServerUuid(nvmeServerInventory.getUuid());
        refInventory.setClusterUuid(cluster.getUuid());
        refInventory.setCreateDate(new Timestamp(DocUtils.date));
        refInventory.setLastOpDate(new Timestamp(DocUtils.date));

        nvmeServerInventory.setNvmeClusterRefs(asList(refInventory));

        event.setInventory(nvmeServerInventory);
        event.setSuccess(true);
        return event;
    }
}
