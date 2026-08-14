package org.zstack.header.network.l2;

/**
 */
public enum L2Errors {
    ATTACH_ERROR(1000),
    ALLOCATE_VNI_ERROR(1001);

    private String code;

    private L2Errors(int id) {
        code = String.format("L2.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
