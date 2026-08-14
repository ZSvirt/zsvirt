package org.zstack.header.host;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 4/24/20.
 */
@RestResponse(allTo = "inventories")
public class APIQueryHostNetworkBondingReply extends APIQueryReply {
    private List<HostNetworkBondingInventory> inventories;

    public List<HostNetworkBondingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostNetworkBondingInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostNetworkBondingReply __example__() {
        APIQueryHostNetworkBondingReply reply = new APIQueryHostNetworkBondingReply();

        String bondingUuid = uuid();
        String hostUuid = uuid();

        HostNetworkBondingInventory bond = new HostNetworkBondingInventory();
        bond.setUuid(bondingUuid);
        bond.setHostUuid(hostUuid);
        bond.setBondingName("bond0");
        bond.setIpAddresses(Collections.singletonList("172.20.0.116/16"));
        bond.setMac("ac:1f:6b:93:6c:8c");
        bond.setMiimon(100L);
        bond.setMiiStatus("up");
        bond.setMode("active-backup 1");
        bond.setXmitHashPolicy("layer2 0");
        bond.setType("LinuxBonding");
        bond.setAllSlavesActive(true);
        bond.setCreateDate(new Timestamp(DocUtils.date));
        bond.setLastOpDate(new Timestamp(DocUtils.date));

        HostNetworkInterfaceInventory slave1 = new HostNetworkInterfaceInventory();
        slave1.setUuid(uuid());
        slave1.setHostUuid(hostUuid);
        slave1.setBondingUuid(bondingUuid);
        slave1.setInterfaceName("eno1");
        slave1.setSpeed(1000L);
        slave1.setCarrierActive(true);
        slave1.setSlaveActive(true);
        slave1.setMac(bond.getMac());
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
        slave2.setMac(bond.getMac());
        slave2.setInterfaceType(NetworkInterfaceType.bondingSlave.toString());
        slave2.setCreateDate(new Timestamp(DocUtils.date));
        slave2.setLastOpDate(new Timestamp(DocUtils.date));

        bond.setSlaves(Arrays.asList(slave1, slave2));
        reply.setInventories(Collections.singletonList(bond));
        return reply;
    }
}
