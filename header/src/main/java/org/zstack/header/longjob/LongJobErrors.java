package org.zstack.header.longjob;

public enum LongJobErrors {
    CANCELED(1000),
    NONCANCELABLE(1001),
    INTERRUPTED(1002),
    NOT_SUPPORTED(1003);

    private String code;

    LongJobErrors(int id) {
        code = String.format("LONG_JOB.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
