package org.zstack.network.service.virtualrouter;

/**
 */
public enum VirtualRouterErrors {
    NOT_IN_CORRECT_STATE(1000),
    NO_PUBLIC_NETWORK_IN_OFFERING(1001),
    NO_DEFAULT_OFFERING(1002);

    private String code;

    private VirtualRouterErrors(int id) {
        code = String.format("VR.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
