package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class ForwardDnsStruct {
    private String hostUuid;
    private L3NetworkInventory l3Network;
    private String mac;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }
}
