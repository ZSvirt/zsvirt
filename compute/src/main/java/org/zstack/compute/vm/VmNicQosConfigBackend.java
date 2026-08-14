package org.zstack.compute.vm;

public interface VmNicQosConfigBackend {
    String getVmInstanceType();
    void addNicQos(String vmUuid, String vmNicUuid, Long outboundBandwidth, Long inboundBandwidth);
    void deleteNicQos(String vmUuid, String vmNicUuid,String direction);
    VmNicQosStruct getNicQos(String vmUuid, String vmNicUuid);

    void addVmQos(String vmUuid, Long outboundBandwidth, Long inboundBandwidth);
    void deleteVmQos(String vmUuid, String direction);
    VmNicQosStruct getVmQos(String vmUuid);
}
