package org.zstack.storage.primary.block;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/5/25 13:26
 */
public enum LunErrors {
    GENERIC_ERROR(1000),

    LUN_HAS_BEEN_CREATED(1001),
    LUN_CAN_NOT_BE_FOUND(1002),
    ;

    private String code;

    private LunErrors(int id) {
        this.code = String.format("LUN.%s", id);
    }

    public String toString() {
        return this.code;
    }


}
