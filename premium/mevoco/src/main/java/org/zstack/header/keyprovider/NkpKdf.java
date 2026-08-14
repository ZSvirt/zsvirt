package org.zstack.header.keyprovider;

public enum NkpKdf {
    HKDF_SHA256("HKDF-SHA256");

    private final String value;

    NkpKdf(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
