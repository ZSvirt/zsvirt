package org.zstack.core.cloudbus;

/**
 */
public class BusQueue {
    private final String name;
    private final String bindingKey;
    private final BusExchange busExchange;

    public BusQueue(String name, String bindingKey, BusExchange busExchange) {
        this.name = name;
        this.bindingKey = bindingKey;
        this.busExchange = busExchange;
    }

    public BusQueue(String name, BusExchange busExchange) {
        this.name = name;
        this.bindingKey = name;
        this.busExchange = busExchange;
    }

    public String getName() {
        return name;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public BusExchange getBusExchange() {
        return busExchange;
    }
}
