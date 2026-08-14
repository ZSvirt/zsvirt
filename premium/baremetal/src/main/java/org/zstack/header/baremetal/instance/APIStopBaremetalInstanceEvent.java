package org.zstack.header.baremetal.instance;

import org.zstack.header.baremetal.network.BaremetalNicInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 7/6/18.
 */
@RestResponse(allTo = "inventory")
public class APIStopBaremetalInstanceEvent extends APIEvent {
    private BaremetalInstanceInventory inventory;

    public APIStopBaremetalInstanceEvent() {
    }

    public APIStopBaremetalInstanceEvent(String apiId) {
        super(apiId);
    }

    public BaremetalInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIStopBaremetalInstanceEvent __example__() {
        APIStopBaremetalInstanceEvent event = new APIStopBaremetalInstanceEvent();
        BaremetalInstanceInventory bm = new BaremetalInstanceInventory();
        bm.setUuid(uuid());
        bm.setName("BM-1");
        bm.setDescription("This is a baremetal instance.");
        bm.setZoneUuid(uuid());
        bm.setClusterUuid(uuid());
        bm.setChassisUuid(uuid());
        bm.setImageUuid(uuid());
        bm.setPlatform("Linux");
        bm.setState(BaremetalInstanceState.Stopped.toString());
        bm.setStatus(BaremetalInstanceStatus.Provisioned.toString());
        bm.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        bm.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        BaremetalNicInventory nic = new BaremetalNicInventory();
        nic.setUuid(uuid());
        nic.setBaremetalInstanceUuid(bm.getUuid());
        nic.setL3NetworkUuid(uuid());
        nic.setMac("6c:b3:11:1b:0b:1e");
        nic.setIp("192.168.0.10");
        nic.setNetmask("255.255.255.0");
        nic.setGateway("192.168.0.1");
        nic.setPxe(false);
        bm.setBmNics(asList(nic));

        event.setInventory(bm);
        return event;
    }
}
