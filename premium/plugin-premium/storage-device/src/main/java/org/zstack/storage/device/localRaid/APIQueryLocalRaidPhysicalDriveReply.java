package org.zstack.storage.device.localRaid;
import java.sql.Timestamp;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryLocalRaidPhysicalDriveReply extends APIQueryReply {
    private List<RaidPhysicalDriveInventory> inventories;

    public List<RaidPhysicalDriveInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<RaidPhysicalDriveInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryLocalRaidPhysicalDriveReply __example__() {
        APIQueryLocalRaidPhysicalDriveReply reply = new APIQueryLocalRaidPhysicalDriveReply();

        String raidControllerUuid = uuid();

        RaidPhysicalDriveInventory inventory1 = new RaidPhysicalDriveInventory();
        inventory1.setUuid(uuid());
        inventory1.setName("raidPhysical");
        inventory1.setRaidLevel("raid1");
        inventory1.setRaidControllerUuid(raidControllerUuid);
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
        inventory1.setName("raidPhysical");
        inventory1.setRaidLevel("raid1");
        inventory1.setRaidControllerUuid(raidControllerUuid);
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

        reply.setInventories(asList(inventory1, inventory2));
        reply.setSuccess(true);
        return reply;
    }
}
