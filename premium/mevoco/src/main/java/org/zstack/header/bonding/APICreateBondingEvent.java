package org.zstack.header.bonding;

import org.zstack.header.host.NetworkInterfaceType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventory")
public class APICreateBondingEvent extends APIEvent {
    public APICreateBondingEvent() {
    }

    public APICreateBondingEvent(String apiId) {
        super(apiId);
    }

    public List<HostNetworkBondingInventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<HostNetworkBondingInventory> inventory) {
        this.inventory = inventory;
    }

    private List<HostNetworkBondingInventory> inventory;

    public static APICreateBondingEvent __example__() {
        APICreateBondingEvent evt = new APICreateBondingEvent();

        HostNetworkBondingInventory bonding = new HostNetworkBondingInventory();
        bonding.setHostUuid(uuid());
        bonding.setBondingName("test");
        bonding.setType("LinuxBonding");
        bonding.setMode("802.3ad");
        bonding.setXmitHashPolicy("layer2+3");
        bonding.setIpAddresses(Arrays.asList("192.168.1.1".split(",")));

        HostNetworkInterfaceInventory slave1 = new HostNetworkInterfaceInventory();
        slave1.setUuid(uuid());
        slave1.setHostUuid(bonding.getHostUuid());
        slave1.setBondingUuid(bonding.getUuid());
        slave1.setInterfaceName("eno1");
        slave1.setSpeed(1000L);
        slave1.setCarrierActive(true);
        slave1.setSlaveActive(true);
        slave1.setInterfaceType(NetworkInterfaceType.bondingSlave.toString());
        slave1.setCreateDate(new Timestamp(DocUtils.date));
        slave1.setLastOpDate(new Timestamp(DocUtils.date));
        bonding.setSlaves(Collections.singletonList(slave1));

        evt.setInventory(Arrays.asList(bonding));
        return evt;
    }
}
