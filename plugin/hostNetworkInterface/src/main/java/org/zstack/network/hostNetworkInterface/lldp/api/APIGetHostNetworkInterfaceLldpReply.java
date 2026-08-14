package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefInventory;

@RestResponse(fieldsTo = {"all"})
public class APIGetHostNetworkInterfaceLldpReply extends APIReply {
    HostNetworkInterfaceLldpRefInventory lldp;

    public HostNetworkInterfaceLldpRefInventory getLldp() {
        return lldp;
    }

    public void setLldp(HostNetworkInterfaceLldpRefInventory lldp) {
        this.lldp = lldp;
    }

    public static APIGetHostNetworkInterfaceLldpReply __example__() {
        APIGetHostNetworkInterfaceLldpReply reply = new APIGetHostNetworkInterfaceLldpReply();
        HostNetworkInterfaceLldpRefInventory inv = new HostNetworkInterfaceLldpRefInventory();

        inv.setLldpUuid(uuid());
        inv.setChassisId("mac 00:1e:08:1d:05:ba");
        inv.setTimeToLive(120);
        inv.setManagementAddress("172.25.2.4");
        inv.setSystemName("BM-MN-3");
        inv.setSystemDescription(" CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved.");
        inv.setSystemCapabilities("Bridge, on  Router, on");
        inv.setPortId("ifname eth-0-5");
        inv.setPortDescription("eth-0-4");
        inv.setVlanId(3999);
        inv.setAggregationPortId(4294965248L);
        inv.setMtu(9600);

        reply.setLldp(inv);
        return reply;
    }
}
