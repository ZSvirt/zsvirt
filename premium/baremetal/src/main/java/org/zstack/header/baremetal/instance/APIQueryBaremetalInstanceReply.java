package org.zstack.header.baremetal.instance;

import org.zstack.header.baremetal.network.BaremetalNicInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 7/4/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryBaremetalInstanceReply extends APIQueryReply {
    private List<BaremetalInstanceInventory> inventories;

    public List<BaremetalInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BaremetalInstanceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryBaremetalInstanceReply __example__() {
        APIQueryBaremetalInstanceReply reply = new APIQueryBaremetalInstanceReply();

        BaremetalInstanceInventory bm = new BaremetalInstanceInventory();
        bm.setUuid(uuid());
        bm.setName("BM-1");
        bm.setDescription("This is a baremetal instance.");
        bm.setZoneUuid(uuid());
        bm.setClusterUuid(uuid());
        bm.setChassisUuid(uuid());
        bm.setImageUuid(uuid());
        bm.setPlatform("Linux");
        bm.setState(BaremetalInstanceState.Running.toString());
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

        reply.setInventories(asList(bm));
        return reply;
    }
}
