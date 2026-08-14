package org.zstack.core.cloudbus;

/**
 */
public enum BusExchange {
    P2P("topic"),
    BROADCAST("topic"),
    NO_ROUTE("fanout");

    private BusExchange(String type) {
        this.type = type;
    }

    private final String type;

    public String getType() {
        return type;
    }
}
