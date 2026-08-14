package org.zstack.header.network.l3;

import org.zstack.header.message.APICreateMessage;

public interface IpRangeFactory {
    IpRangeType getType();

    IpRangeInventory createIpRange(IpRangeInventory ipr, APICreateMessage msg);
}
