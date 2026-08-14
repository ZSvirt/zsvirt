package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SnatStruct {
    private String guestIp;
    private String guestGateway;
    private String guestNetmask;
    private String guestMac;
    private L3NetworkInventory l3Network;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(String.format("guest ip: %s,", guestIp));
        sb.append(String.format("guest mac: %s,", guestMac));
        sb.append(String.format("guest netmask: %s,", guestNetmask));
        sb.append(String.format("guest gateway: %s,", guestGateway));
        sb.append(String.format("l3NetworkUuid: %s,", l3Network.getUuid()));
        sb.append("]");
        return sb.toString();
    }

    public String getGuestIp() {
        return guestIp;
    }

    public void setGuestIp(String guestIp) {
        this.guestIp = guestIp;
    }

    public String getGuestGateway() {
        return guestGateway;
    }

    public void setGuestGateway(String guestGateway) {
        this.guestGateway = guestGateway;
    }

    public String getGuestNetmask() {
        return guestNetmask;
    }

    public void setGuestNetmask(String guestNetmask) {
        this.guestNetmask = guestNetmask;
    }

    public String getGuestMac() {
        return guestMac;
    }

    public void setGuestMac(String guestMac) {
        this.guestMac = guestMac;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }
}
