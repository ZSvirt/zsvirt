package org.zstack.network.service.flat;

import org.zstack.header.network.service.NetworkServiceProviderType;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatNetworkServiceConstant {
    public static final String FLAT_NETWORK_SERVICE_TYPE_STRING = "Flat";
    public static final NetworkServiceProviderType FLAT_NETWORK_SERVICE_TYPE = new NetworkServiceProviderType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);

    public static final String SERVICE_ID = "flat.dhcp";

    public static class AgentCmd {
    }

    public static class AgentRsp {
        public boolean success = true;
        public String error;
    }
}
