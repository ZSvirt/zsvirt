package org.zstack.header.search;

public enum SearchErrors {
    GENERAL_ERROR(1000),

    // 2XXX: ZQL Errors
    ZQL_ERROR(2000),
    INVALID_ZQL_FIELDS(2101),

    // 3XXX: Search Errors - TBD
    ;

    public final String code;

    SearchErrors(int id) {
        code = String.format("SEARCH.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
