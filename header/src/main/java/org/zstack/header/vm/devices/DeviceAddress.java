package org.zstack.header.vm.devices;

import org.zstack.utils.gson.JSONObjectUtil;

/**
 * structure to match device address
 */
public class DeviceAddress {
    public String type;

    public String bus;

    // for pci address
    public String domain;
    public String slot;
    public String function;

    // for driver address
    public String controller;
    public String target;
    public String unit;


    public static DeviceAddress fromString(String address) {
        return JSONObjectUtil.toObject(address, DeviceAddress.class);
    }

    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }


}
