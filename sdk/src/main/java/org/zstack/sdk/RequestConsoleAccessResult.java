package org.zstack.sdk;

import org.zstack.sdk.ConsoleInventory;

public class RequestConsoleAccessResult {
    public ConsoleInventory inventory;
    public void setInventory(ConsoleInventory inventory) {
        this.inventory = inventory;
    }
    public ConsoleInventory getInventory() {
        return this.inventory;
    }

}
