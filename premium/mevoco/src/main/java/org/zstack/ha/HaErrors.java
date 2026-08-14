package org.zstack.ha;

/**
 * Create by weiwang at 2018/7/28
 */
public enum HaErrors {
    PRIMARY_STORAGE_ERROR(1000);


    private String code;

    private HaErrors(int id) {
        code = String.format("HA.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
