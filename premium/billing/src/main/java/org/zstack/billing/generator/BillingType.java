package org.zstack.billing.generator;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum BillingType {
    Vm("Vm"),
    CPU("CPU"),
    Memory("Memory"),
    RootVolume("RootVolume"),
    DataVolume("DataVolume"),
    GPU("GPU"),
    PubIpVmNicBandwidth("PubIpVmNicBandwidth"),
    PubIpVmNicBandwidthIn("PubIpVmNicBandwidthIn"),
    PubIpVmNicBandwidthOut("PubIpVmNicBandwidthOut"),
    PubIpVipBandwidth("PubIpVipBandwidth"),
    PubIpVipBandwidthIn("PubIpVipBandwidthIn"),
    PubIpVipBandwidthOut("PubIpVipBandwidthOut"),
    BareMetal2Instance("BareMetal2Instance");

    private String name;

    BillingType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
