package org.zstack.search;

public enum SearchErrors {

    SEARCH_MODULE_DISABLED(1000);

    private String code;

    SearchErrors(int id) {
        code = String.format("SEARCH.%s", id);
    }

    public boolean isEqual(Object t) {
        return t.toString().equals(this.toString());
    }

    @Override
    public String toString() {
        return code;
    }
}
