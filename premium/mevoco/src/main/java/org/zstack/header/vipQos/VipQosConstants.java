package org.zstack.header.vipQos;

import org.zstack.header.network.service.NetworkServiceType;

/**
 * Created by liangbo.zhou on 17-6-19.
 */
public class VipQosConstants {
    public static final String SERVICE_ID= "VipQos";
    public static final NetworkServiceType VIPQOS_NETWORK_SERVICE_TYPE = new NetworkServiceType("VipQos");

    public static final Long VipQosBandWidth_MIN = 1024 * 1024L - 1;
    public static final Long VipQosBandWidth_MAX = 32212254720L;
}
