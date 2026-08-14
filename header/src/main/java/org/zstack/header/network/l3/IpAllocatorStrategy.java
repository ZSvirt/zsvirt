package org.zstack.header.network.l3;

public interface IpAllocatorStrategy {
    IpAllocatorType getType();

    UsedIpInventory allocateIp(IpAllocateMessage msg);
}
