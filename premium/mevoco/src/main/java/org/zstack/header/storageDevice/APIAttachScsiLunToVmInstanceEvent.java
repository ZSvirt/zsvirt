package org.zstack.header.storageDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;

/**
 * Create by weiwang at 2018/8/2
 */

@RestResponse(allTo = "inventory")
public class APIAttachScsiLunToVmInstanceEvent extends APIEvent {
    private ScsiLunInventory inventory;

    public ScsiLunInventory getInventory() {
        return inventory;
    }

    public void setInventory(ScsiLunInventory inventory) {
        this.inventory = inventory;
    }

    public APIAttachScsiLunToVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIAttachScsiLunToVmInstanceEvent() {
        super(null);
    }

    public static APIAttachScsiLunToVmInstanceEvent __example__() {
        APIAttachScsiLunToVmInstanceEvent event = new APIAttachScsiLunToVmInstanceEvent();
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

        scsiLunInventory.setScsiLunVmInstanceRefs(Arrays.asList(scsiLunVmInstanceRefInventory));

        event.setInventory(scsiLunInventory);
        event.setSuccess(true);
        return event;
    }
}
