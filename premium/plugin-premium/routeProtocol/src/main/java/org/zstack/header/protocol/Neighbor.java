package org.zstack.header.protocol;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;

@PythonClassInventory
public class Neighbor implements Serializable {
    private String id;
    private String priority;
    private String state;
    private String deadTime;
    private String neighborAddress;
    private String device;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDeadTime() {
        return deadTime;
    }

    public void setDeadTime(String deadTime) {
        this.deadTime = deadTime;
    }

    public String getNeighborAddress() {
        return neighborAddress;
    }

    public void setNeighborAddress(String neighborAddress) {
        this.neighborAddress = neighborAddress;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
