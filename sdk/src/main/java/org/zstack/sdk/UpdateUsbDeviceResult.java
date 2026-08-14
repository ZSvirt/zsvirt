package org.zstack.sdk;

import org.zstack.sdk.UsbDeviceInventory;

public class UpdateUsbDeviceResult {
    public UsbDeviceInventory inventory;
    public void setInventory(UsbDeviceInventory inventory) {
        this.inventory = inventory;
    }
    public UsbDeviceInventory getInventory() {
        return this.inventory;
    }

}
