package org.zstack.storage.primary.sharedblock;

/**
 * Create by weiwang at 2018/4/11
 */
public enum LvmlockdLockingType {
    NULL(0), SHARE(1), EXCLUSIVE(2);

    private final int value;

    LvmlockdLockingType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
