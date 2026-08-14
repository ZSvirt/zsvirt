package org.zstack.header.network.l3;

public interface IpAllocateMessage {
    String getAllocatorStrategy();

    String getL3NetworkUuid();

    String getRequiredIp();

    String getIpRangeUuid();

    default String getExcludedIp() {
        return null;
    }

    default boolean isDuplicatedIpAllowed() {return false;}

    void setIpRangeUuid(String ipRangeUuid);

    void setRequiredIp(String requiredIp);
}
