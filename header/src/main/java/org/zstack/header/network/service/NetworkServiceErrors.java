package org.zstack.header.network.service;

/**
 */
public enum NetworkServiceErrors {
    ATTACH_NETWORK_SERVICE_PROVIDER_ERROR(1000),
    DETACH_NETWORK_SERVICE_PROVIDER_ERROR(1001);

    private String code;

    private NetworkServiceErrors(int id) {
        code = String.format("NWS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
