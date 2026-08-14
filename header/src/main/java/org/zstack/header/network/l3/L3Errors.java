package org.zstack.header.network.l3;

/**
 */
public enum L3Errors {
    ALLOCATE_IP_ERROR(1000);

    private String code;

    private L3Errors(int id) {
        code = String.format("L3.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
