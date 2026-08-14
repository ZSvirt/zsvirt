package org.zstack.ipsec;

import org.zstack.header.network.service.NetworkServiceType;

/**
 * Created by xing5 on 2016/11/3.
 */
public interface IPsecConstants {
    String SERVICE_ID = "ipsec";
    int[] IKE_DH_GROUP= {2,5,14,15,16,17,18,19,20,21,22,23,24,25,26};
    NetworkServiceType IPSEC_NETWORK_SERVICE_TYPE = new NetworkServiceType("IPsec");
    public static final String ACTION_CATEGORY = "ipsec";
    public static final long IPSEC_UDP_PORT_68 = 68;
    public static final long IPSEC_UDP_PORT_500 = 500;
    public static final long IPSEC_UDP_PORT_4500 = 4500;
    public static final String IPSEC_PROTOCOL_UDP = "udp";
    public static final String IPSEC_PROTOCOL_TCP = "tcp";
    public static final String IPSEC_PROTOCOL_HTTP = "http";
    public static final String IPSEC_PROTOCOL_HTTPS = "https";
    public static final String IPSEC_STATE_UP = "up";
    public static final String IPSEC_STATE_DOWN = "down";

    enum Param {
        BACKEND_ACTION_TYPE,
    }

    enum IPsecBackendAction {
        CREATE,
        SYNC,
        DELETE,
        RECONNECT,
        NONE,
    }
}
