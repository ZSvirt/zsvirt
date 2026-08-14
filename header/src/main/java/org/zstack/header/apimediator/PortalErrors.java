package org.zstack.header.apimediator;

/**
 */
public enum PortalErrors {
    NO_SERVICE_FOR_MESSAGE(1000),
    MISSING_FIELD(1001);

    private String code;

    private PortalErrors(int id) {
        code = String.format("PORTAL.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
