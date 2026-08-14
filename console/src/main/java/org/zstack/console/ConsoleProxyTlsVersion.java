package org.zstack.console;

public enum ConsoleProxyTlsVersion {
    NONE("default"),
    TLSV1_1("tlsv1_1"),
    TLSV1_2("tlsv1_2");

    public final String tlsVersion;

    ConsoleProxyTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    public String toCommandParameter() {
        return tlsVersion;
    }
}
