package org.zstack.core.asyncbatch;

public enum WhileMode {
    EACH(1), STEP(2);
    private final int VALUE;

    WhileMode(int VALUE) {
        this.VALUE = VALUE;
    }

    public int getNumVal() {
        return VALUE;
    }
}
