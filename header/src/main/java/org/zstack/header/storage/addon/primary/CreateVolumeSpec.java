package org.zstack.header.storage.addon.primary;

import org.zstack.header.volume.VolumeQos;

public class CreateVolumeSpec {
    private String name;
    private String uuid;
    private long size;
    private VolumeQos qos;

    private String allocatedUrl;
    private boolean dryRun;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public VolumeQos getQos() {
        return qos;
    }

    public void setQos(VolumeQos qos) {
        this.qos = qos;
    }

    public String getAllocatedUrl() {
        return allocatedUrl;
    }

    public void setAllocatedUrl(String allocatedUrl) {
        this.allocatedUrl = allocatedUrl;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
