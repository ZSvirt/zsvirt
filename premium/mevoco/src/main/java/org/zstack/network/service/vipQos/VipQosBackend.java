package org.zstack.network.service.vipQos;

import org.zstack.header.core.Completion;
import org.zstack.header.vipQos.VipQosStruct;

import java.util.List;

/**
 * Created by liangbo.zhou on 17-6-20.
 */
public interface VipQosBackend {
    void setVipQos(List<VipQosStruct> structs, String vrUuid, Completion completion);

    void deleteVipQos(List<VipQosStruct> structs, Completion completion);

    void deleteVipAllQos(List<VipQosStruct> structs, Completion completion);

    String getNetworkServiceProviderType();
}
