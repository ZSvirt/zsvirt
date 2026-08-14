package org.zstack.header.keyprovider;

public enum NkpSaltPolicy {
    PROVIDER_NAME("providerName");

    private final String value;

    NkpSaltPolicy(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
