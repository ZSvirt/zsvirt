package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class GetHostNetworkFactsReply extends MessageReply {
    List<HostNetworkBondingInventory> bondings;
    List<HostNetworkInterfaceInventory> nics;

    public List<HostNetworkInterfaceInventory> getNics() {
        return nics;
    }

    public void setNics(List<HostNetworkInterfaceInventory> nics) {
        this.nics = nics;
    }

    public List<HostNetworkBondingInventory> getBondings() {
        return bondings;
    }

    public void setBondings(List<HostNetworkBondingInventory> bondings) {
        this.bondings = bondings;
    }
}
