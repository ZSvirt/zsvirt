package org.zstack.storage.device.localRaid;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/10/18
 */

@RestResponse(allTo = "inventories")
public class APIRefreshLocalRaidEvent extends APIEvent {
    private List<RaidControllerInventory> inventories;

    public List<RaidControllerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<RaidControllerInventory> inventories) {
        this.inventories = inventories;
    }

    public APIRefreshLocalRaidEvent() {
    }

    public APIRefreshLocalRaidEvent(String apiId) {
        super(apiId);
    }

    public static APIRefreshLocalRaidEvent __example__() {
        APIRefreshLocalRaidEvent evt = new APIRefreshLocalRaidEvent();

        RaidControllerInventory controller = new RaidControllerInventory();
        controller.setUuid(uuid());
        controller.setName("LSI 2208 Raid Controller");
        controller.setDescription("this is a description");
        controller.setProductName("LSI 2208 MegaRAID");
        controller.setSasAddress("500304801f948100");
        controller.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        controller.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        RaidPhysicalDriveInventory inventory1 = new RaidPhysicalDriveInventory();
        inventory1.setUuid(uuid());
        inventory1.setName("raidPhysicalDrive1");
        inventory1.setRaidLevel("raid1");
        inventory1.setRaidControllerUuid(controller.getUuid());
        inventory1.setEnclosureDeviceId(252);
        inventory1.setSlotNumber(0);
        inventory1.setDiskGroup(0);
        inventory1.setWwn("50014ee059f2f0c0");
        inventory1.setSerialNumber("WMC6M0K8519Y");
        inventory1.setDeviceModel("HGST HUS722T1TALA604");
        inventory1.setSize(1000204886016L);
        inventory1.setDriveState("Online, Spun Up");
        inventory1.setLocateStatus(LocateStatus.Disabled);
        inventory1.setDriveType("SATA");
        inventory1.setMediaType("HDD");
        inventory1.setRotationRate(7200);
        inventory1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory1.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        RaidPhysicalDriveInventory inventory2 = new RaidPhysicalDriveInventory();
        inventory1.setUuid(uuid());
        inventory1.setName("raidPhysicalDrive2");
        inventory1.setRaidLevel("raid1");
        inventory1.setRaidControllerUuid(controller.getUuid());
        inventory1.setEnclosureDeviceId(252);
        inventory1.setSlotNumber(1);
        inventory1.setDiskGroup(0);
        inventory1.setWwn("50014ee059f2f491");
        inventory1.setSerialNumber("V6GLEX2S");
        inventory1.setDeviceModel("HGST HUS726T4TALA6L4");
        inventory1.setSize(1000204886016L);
        inventory1.setDriveState("Online, Spun Up");
        inventory1.setLocateStatus(LocateStatus.Disabled);
        inventory1.setDriveType("SATA");
        inventory1.setMediaType("HDD");
        inventory1.setRotationRate(7200);
        inventory1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory1.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        evt.setInventories(Arrays.asList(controller));
        evt.setSuccess(true);
        return evt;
    }
}
