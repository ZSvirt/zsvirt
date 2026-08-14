package org.zstack.network.service.vipQos;

import org.zstack.header.vipQos.VipQosInventory;
import org.zstack.header.vipQos.VipQosStruct;

/**
 * Created by liangbo.zhou on 17-6-29.
 */
public interface VipQosManager {
    VipQosStruct getVipQosStruct(VipQosInventory inv);
}
