package org.zstack.header.keyprovider;

public enum KmipVersion {
    V1_0("1.0"),
    V1_1("1.1"),
    V1_2("1.2"),
    V1_3("1.3"),
    V1_4("1.4"),
    V2_0("2.0"),
    V2_1("2.1"),
    ;

    private final String value;

    KmipVersion(String value) {
        this.value = value;
    }

    public static KmipVersion fromString(String value) {
        if (value == null) {
            return null;
        }
        for (KmipVersion version : values()) {
            if (version.value.equals(value)) {
                return version;
            }
        }
        throw new IllegalArgumentException(String.format("unknown kmip version[%s]", value));
    }

    @Override
    public String toString() {
        return value;
    }
}
