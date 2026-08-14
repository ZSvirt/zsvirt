package org.zstack.header.storageDevice;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryScsiLunReply extends APIQueryReply {
    private List<ScsiLunInventory> inventories;

    public List<ScsiLunInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ScsiLunInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryScsiLunReply __example__() {
        APIQueryScsiLunReply reply = new APIQueryScsiLunReply();

        ScsiLunInventory scsiLunInventory = new ScsiLunInventory();
        scsiLunInventory.setName("fc-lun-36b083fe000daf018000022905ba35d8f");
        scsiLunInventory.setUuid(uuid());
        scsiLunInventory.setWwn("0x6f01faf000d5c3e7");
        scsiLunInventory.setModel("MD32xx");
        scsiLunInventory.setVendor("DELL");
        scsiLunInventory.setType("mpath");
        scsiLunInventory.setSerial("6b083fe000daf018000015505abbe00a");
        scsiLunInventory.setSize(5497558138880l);
        scsiLunInventory.setWwid("36b083fe000daf018000022905ba35d8f");
        scsiLunInventory.setPath("pci-0000:05:00.0-fc-0x2012b083fedaf018-lun-11");
        scsiLunInventory.setCreateDate(new Timestamp(DocUtils.date));
        scsiLunInventory.setLastOpDate(new Timestamp(DocUtils.date));

        ScsiLunHostRefInventory scsiLunHostRefInventory = new ScsiLunHostRefInventory();
        scsiLunHostRefInventory.setScsiLunUuid(scsiLunInventory.getUuid());
        scsiLunHostRefInventory.setHostUuid(uuid());
        scsiLunHostRefInventory.setId(1l);
        scsiLunHostRefInventory.setCreateDate(new Timestamp(DocUtils.date));
        scsiLunHostRefInventory.setCreateDate(new Timestamp(DocUtils.date));

        scsiLunInventory.setScsiLunHostRefs(Arrays.asList(scsiLunHostRefInventory));

        ScsiLunVmInstanceRefInventory scsiLunVmInstanceRefInventory = new ScsiLunVmInstanceRefInventory();
        scsiLunVmInstanceRefInventory.setScsiLunUuid(scsiLunInventory.getUuid());
        scsiLunVmInstanceRefInventory.setVmInstanceUuid(uuid());
        scsiLunVmInstanceRefInventory.setId(1l);
        scsiLunVmInstanceRefInventory.setCreateDate(new Timestamp(DocUtils.date));
        scsiLunVmInstanceRefInventory.setCreateDate(new Timestamp(DocUtils.date));

        ScsiLunInventory scsiLunInventory2 = new ScsiLunInventory();
        scsiLunInventory2.setName("iscsi-lun-scsi-14f504e46494c45524d5342436a6c2d4a4443672d30593032");
        scsiLunInventory2.setUuid(uuid());
        scsiLunInventory2.setModel("VIRTUAL-DISK");
        scsiLunInventory2.setVendor("OPNFILER");
        scsiLunInventory2.setType("disk");
        scsiLunInventory2.setSerial("4f504e46494c45524d5342436a6c2d4a4443672d30593032");
        scsiLunInventory2.setSize(5497558138880l);
        scsiLunInventory2.setWwid("scsi-14f504e46494c45524d5342436a6c2d4a4443672d30593032");
        scsiLunInventory2.setPath("ip-10.0.104.213:3260-iscsi-iqn.2018-09.io.zstack:tsn.0000003-lun-0");
        scsiLunInventory2.setCreateDate(new Timestamp(DocUtils.date));
        scsiLunInventory2.setLastOpDate(new Timestamp(DocUtils.date));

        scsiLunHostRefInventory.setScsiLunUuid(scsiLunInventory.getUuid());
        scsiLunHostRefInventory.setHostUuid(uuid());
        scsiLunHostRefInventory.setId(1l);
        scsiLunHostRefInventory.setCreateDate(new Timestamp(DocUtils.date));
        scsiLunHostRefInventory.setCreateDate(new Timestamp(DocUtils.date));

        scsiLunInventory.setScsiLunHostRefs(Arrays.asList(scsiLunHostRefInventory));

        reply.setInventories(asList(scsiLunInventory, scsiLunInventory2));
        reply.setSuccess(true);
        return reply;
    }
}
