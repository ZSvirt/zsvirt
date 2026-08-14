package org.zstack.header.vm;

/**
 * Created by MaJin on 2020/7/9.
 */
public interface NewVmInstanceMessage2 extends NewVmInstanceMessage {
    Integer getCpuNum();
    Long getMemorySize();
    Long getReservedMemorySize();
    String getZoneUuid();
    String getClusterUuid();
    String getHostUuid();
    String getStrategy();
    String getInstanceOfferingUuid();

    void setCpuNum(Integer cpuNum);
    void setMemorySize(Long memorySize);
    void setReservedMemorySize(Long reservedMemorySize);
    void setZoneUuid(String zoneUuid);
    void setClusterUuid(String clusterUuid);
    void setDefaultL3NetworkUuid(String defaultL3NetworkUuid);
    void setType(String type);
}
