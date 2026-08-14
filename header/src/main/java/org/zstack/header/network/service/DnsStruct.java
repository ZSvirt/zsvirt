package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DnsStruct {
    private List<String> dns;
    private L3NetworkInventory l3Network;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(String.format("dns: %s", dns));
        sb.append(String.format("l3NetworkUuid: %s,", l3Network.getUuid()));
        sb.append("]");
        return sb.toString();
    }

    public List<String> getDns() {
        return dns;
    }

    public void setDns(List<String> dns) {
        this.dns = dns;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }
}
