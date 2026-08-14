package org.zstack.network.service.vip;

/**
 */
public interface VipConstant {
    public static final String SERVICE_ID = "vip";
    public static final String ACTION_CATEGORY = "vip";


    public static final String PROTOCOL_TCP = "TCP";
    public static final String PROTOCOL_UDP = "UDP";
    public static final int MAX_PORT = 65535;


    public static enum Params {
        VIP,
        VIP_USE_FOR,
        VIP_SERVICE_PROVIDER_TYPE,
        GUEST_L3NETWORK_VIP_FOR,
    }
}
