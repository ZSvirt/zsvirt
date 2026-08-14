package org.zstack.storage.device.fibreChannel;

import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storageDevice.ScsiLunHostRefInventory;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryFiberChannelStorageReply extends APIQueryReply {
    private List<FiberChannelStorageInventory> inventories;

    public List<FiberChannelStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<FiberChannelStorageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryFiberChannelStorageReply __example__() {
        APIQueryFiberChannelStorageReply reply = new APIQueryFiberChannelStorageReply();

        FiberChannelLunInventory fiberChannelLunInventory11 = new FiberChannelLunInventory();
        fiberChannelLunInventory11.setName("fc-lun-36b083fe000daf018000022905ba35d8f");
        fiberChannelLunInventory11.setFiberChannelStorageUuid(uuid());
        fiberChannelLunInventory11.setUuid(uuid());
        fiberChannelLunInventory11.setWwn("0x6f01faf000d5c3e7");
        fiberChannelLunInventory11.setModel("MD32xx");
        fiberChannelLunInventory11.setVendor("DELL");
        fiberChannelLunInventory11.setType("mpath");
        fiberChannelLunInventory11.setSerial("6b083fe000daf018000015505abbe00a");
        fiberChannelLunInventory11.setSize(5497558138880l);
        fiberChannelLunInventory11.setWwid("36b083fe000daf018000022905ba35d8f");
        fiberChannelLunInventory11.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-11");

        FiberChannelLunInventory fiberChannelLunInventory21 = new FiberChannelLunInventory();
        fiberChannelLunInventory21.setName("fc-lun-36b083fe000daf018000022905ba35d8f");
        fiberChannelLunInventory21.setFiberChannelStorageUuid(uuid());
        fiberChannelLunInventory21.setUuid(uuid());
        fiberChannelLunInventory21.setWwn("0x6f01faf000d5c3e7");
        fiberChannelLunInventory21.setModel("MD32xx");
        fiberChannelLunInventory21.setVendor("DELL");
        fiberChannelLunInventory21.setType("mpath");
        fiberChannelLunInventory21.setSerial("6b083fe000daf018000015505abbe00a");
        fiberChannelLunInventory21.setSize(5497558138880l);
        fiberChannelLunInventory21.setWwid("36b083fe000daf018000022905ba35d8f");
        fiberChannelLunInventory21.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-11");

        FiberChannelLunInventory fiberChannelLunInventory12 = new FiberChannelLunInventory();
        fiberChannelLunInventory12.setName("fc-lun-36f01faf000d5c3e7000023ef5ba362f2");
        fiberChannelLunInventory12.setFiberChannelStorageUuid(uuid());
        fiberChannelLunInventory12.setUuid(uuid());
        fiberChannelLunInventory12.setWwn("0x6b083fe000daf018");
        fiberChannelLunInventory12.setModel("MD32xx");
        fiberChannelLunInventory12.setVendor("DELL");
        fiberChannelLunInventory12.setType("mpath");
        fiberChannelLunInventory12.setSerial("6b083fe000daf018000015505abbe00a");
        fiberChannelLunInventory12.setSize(5497558138880l);
        fiberChannelLunInventory12.setWwid("36f01faf000d5c3e7000023ef5ba362f2");
        fiberChannelLunInventory12.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-12");

        FiberChannelLunInventory fiberChannelLunInventory22 = new FiberChannelLunInventory();
        fiberChannelLunInventory22.setName("fc-lun-36f01faf000d5c3e7000023ef5ba362f2");
        fiberChannelLunInventory22.setFiberChannelStorageUuid(uuid());
        fiberChannelLunInventory22.setUuid(uuid());
        fiberChannelLunInventory22.setWwn("0x6b083fe000daf018");
        fiberChannelLunInventory22.setModel("MD32xx");
        fiberChannelLunInventory22.setVendor("DELL");
        fiberChannelLunInventory22.setType("mpath");
        fiberChannelLunInventory22.setSerial("6b083fe000daf018000015505abbe00a");
        fiberChannelLunInventory22.setSize(5497558138880l);
        fiberChannelLunInventory22.setWwid("36f01faf000d5c3e7000023ef5ba362f2");
        fiberChannelLunInventory22.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-12");

        FiberChannelStorageInventory fiberChannelStorageInventory1 = new FiberChannelStorageInventory();
        fiberChannelStorageInventory1.setUuid(uuid());
        fiberChannelStorageInventory1.setState(StorageDeviceState.Enabled.toString());
        fiberChannelStorageInventory1.setCreateDate(new Timestamp(DocUtils.date));
        fiberChannelStorageInventory1.setLastOpDate(new Timestamp(DocUtils.date));
        fiberChannelStorageInventory1.setFiberChannelLuns(Arrays.asList(fiberChannelLunInventory11, fiberChannelLunInventory12));
        fiberChannelLunInventory11.setFiberChannelStorageUuid(fiberChannelStorageInventory1.getUuid());
        fiberChannelLunInventory12.setFiberChannelStorageUuid(fiberChannelStorageInventory1.getUuid());

        FiberChannelStorageInventory fiberChannelStorageInventory2 = new FiberChannelStorageInventory();
        fiberChannelStorageInventory2.setUuid(uuid());
        fiberChannelStorageInventory2.setState(StorageDeviceState.Enabled.toString());
        fiberChannelStorageInventory2.setCreateDate(new Timestamp(DocUtils.date));
        fiberChannelStorageInventory2.setLastOpDate(new Timestamp(DocUtils.date));
        fiberChannelStorageInventory1.setFiberChannelLuns(Arrays.asList(fiberChannelLunInventory21, fiberChannelLunInventory22));
        fiberChannelLunInventory21.setFiberChannelStorageUuid(fiberChannelStorageInventory1.getUuid());
        fiberChannelLunInventory22.setFiberChannelStorageUuid(fiberChannelStorageInventory1.getUuid());

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

        ScsiLunHostRefInventory refInventory11 = new ScsiLunHostRefInventory();
        refInventory11.setId(1);
        refInventory11.setScsiLunUuid(fiberChannelLunInventory11.getUuid());
        refInventory11.setHostUuid(hi.getUuid());
        refInventory11.setCreateDate(new Timestamp(DocUtils.date));
        refInventory11.setLastOpDate(new Timestamp(DocUtils.date));

        ScsiLunHostRefInventory refInventory12 = new ScsiLunHostRefInventory();
        refInventory12.setId(2);
        refInventory12.setScsiLunUuid(fiberChannelLunInventory12.getUuid());
        refInventory12.setHostUuid(hi.getUuid());
        refInventory12.setCreateDate(new Timestamp(DocUtils.date));
        refInventory12.setLastOpDate(new Timestamp(DocUtils.date));

        ScsiLunHostRefInventory refInventory21 = new ScsiLunHostRefInventory();
        refInventory21.setId(3);
        refInventory21.setScsiLunUuid(fiberChannelLunInventory12.getUuid());
        refInventory21.setHostUuid(hi.getUuid());
        refInventory21.setCreateDate(new Timestamp(DocUtils.date));
        refInventory21.setLastOpDate(new Timestamp(DocUtils.date));

        ScsiLunHostRefInventory refInventory22 = new ScsiLunHostRefInventory();
        refInventory22.setId(3);
        refInventory22.setScsiLunUuid(fiberChannelLunInventory12.getUuid());
        refInventory22.setHostUuid(hi.getUuid());
        refInventory22.setCreateDate(new Timestamp(DocUtils.date));
        refInventory22.setLastOpDate(new Timestamp(DocUtils.date));

        fiberChannelLunInventory11.setScsiLunHostRefs(Arrays.asList(refInventory11));
        fiberChannelLunInventory12.setScsiLunHostRefs(Arrays.asList(refInventory12));
        fiberChannelLunInventory21.setScsiLunHostRefs(Arrays.asList(refInventory21));
        fiberChannelLunInventory22.setScsiLunHostRefs(Arrays.asList(refInventory22));

        reply.setInventories(asList(fiberChannelStorageInventory1, fiberChannelStorageInventory2));
        reply.setSuccess(true);
        return reply;
    }
}
