package org.zstack.header.tag;

public enum TagErrors {
    GENERIC_ERROR(1000),
    DUPLICATED_TAG(1001),
    TAG_QUOTA_EXCEEDED(1002),
    ;

    private String code;

    TagErrors(int id) {
        code = String.format("TAG.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
