package org.zstack.header.host;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 4/24/20.
 */
@RestResponse(allTo = "inventories")
public class APIQueryHostNetworkInterfaceReply extends APIQueryReply {
    private List<HostNetworkInterfaceInventory> inventories;

    public List<HostNetworkInterfaceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostNetworkInterfaceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostNetworkInterfaceReply __example__() {
        APIQueryHostNetworkInterfaceReply reply = new APIQueryHostNetworkInterfaceReply();

        String bondingUuid = uuid();
        String hostUuid = uuid();

        HostNetworkInterfaceInventory slave1 = new HostNetworkInterfaceInventory();
        slave1.setUuid(uuid());
        slave1.setHostUuid(hostUuid);
        slave1.setBondingUuid(bondingUuid);
        slave1.setInterfaceName("eno1");
        slave1.setSpeed(1000L);
        slave1.setCarrierActive(true);
        slave1.setSlaveActive(true);
        slave1.setMac("98:03:9b:00:ea:f1");
        slave1.setInterfaceType(NetworkInterfaceType.bondingSlave.toString());
        slave1.setCreateDate(new Timestamp(DocUtils.date));
        slave1.setLastOpDate(new Timestamp(DocUtils.date));

        HostNetworkInterfaceInventory slave2 = new HostNetworkInterfaceInventory();
        slave2.setUuid(uuid());
        slave2.setHostUuid(hostUuid);
        slave2.setBondingUuid(bondingUuid);
        slave2.setInterfaceName("eno2");
        slave2.setSpeed(1000L);
        slave2.setCarrierActive(false);
        slave2.setSlaveActive(false);
        slave2.setMac("98:03:9b:00:ea:f1");
        slave2.setInterfaceType(NetworkInterfaceType.bondingSlave.toString());
        slave2.setCreateDate(new Timestamp(DocUtils.date));
        slave2.setLastOpDate(new Timestamp(DocUtils.date));

        HostNetworkInterfaceInventory ninv1 = new HostNetworkInterfaceInventory();
        ninv1.setUuid(uuid());
        ninv1.setHostUuid(hostUuid);
        ninv1.setInterfaceName("ens2f0");
        ninv1.setSpeed(1000L);
        ninv1.setCarrierActive(true);
        ninv1.setSlaveActive(true);
        ninv1.setIpAddresses(Collections.singletonList("169.254.0.115/24"));
        ninv1.setMac("98:03:9b:00:ea:f2");
        ninv1.setInterfaceType(NetworkInterfaceType.noMaster.toString());
        ninv1.setCreateDate(new Timestamp(DocUtils.date));
        ninv1.setLastOpDate(new Timestamp(DocUtils.date));

        HostNetworkInterfaceInventory ninv2 = new HostNetworkInterfaceInventory();
        ninv2.setUuid(uuid());
        ninv2.setHostUuid(hostUuid);
        ninv2.setInterfaceName("ens2f1");
        ninv2.setSpeed(1000L);
        ninv2.setCarrierActive(false);
        ninv2.setSlaveActive(false);
        ninv2.setMac("98:03:9b:00:ea:f3");
        ninv2.setInterfaceType(NetworkInterfaceType.bridgeSlave.toString());
        ninv2.setCreateDate(new Timestamp(DocUtils.date));
        ninv2.setLastOpDate(new Timestamp(DocUtils.date));

        reply.setInventories(Arrays.asList(slave1, slave2, ninv1, ninv2));
        return reply;
    }
}
