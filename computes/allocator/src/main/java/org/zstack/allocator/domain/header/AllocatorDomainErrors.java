package org.zstack.allocator.domain.header;

public enum AllocatorDomainErrors {
    GENERAL_ERROR(1000),

    // 2XXX: DOMAIN_RELATED_ERROR
    DOMAIN_RELATED_ERROR(2000),
    DOMAIN_NOT_FOUND(2001),
    DOMAIN_IN_USE(2002),

    // 3XXX: RULE_RELATED_ERROR
    RULE_RELATED_ERROR(3000),
    INVALID_RULE(3001),

    // 4XXX: ALLOCATION_RELATED_ERROR
    ALLOCATION_RELATED_ERROR(4000),
    RESOURCE_OUT_OF_DOMAIN(4001),
    DOMAIN_UUID_CHANGE_NOT_ALLOWED(4002),
    ;

    private final String code;

    AllocatorDomainErrors(int id) {
        code = String.format("ALLOCATOR.DOMAIN.%s", id);
    }

    public boolean isEqual(Object t) {
        return t.toString().equals(this.toString());
    }

    @Override
    public String toString() {
        return code;
    }
}
