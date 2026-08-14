package org.zstack.header.network.l2;

/**
 * Created by weiwang on 24/10/2017
 */
public interface L2NetworkGetVniExtensionPoint {
    // Note(WeiW): Get identifier of l2 network, vlan id for vlan network, vsid for nvgre, context id for stt,
    // vni for vxlan and geneve, use vni(Virtual Network Identifier) here for a general name
    Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid);
    String getL2NetworkVniType();
}
