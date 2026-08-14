package org.zstack.storage.primary.sharedblock;


import java.io.Serializable;

/**
 * Create by weiwang at 2018/6/21
 */
public class SharedBlockCandidateStruct implements Serializable {
    public String wwid;
    public String vendor;
    public String model;
    public String wwn;
    public String serial;
    public String hctl;
    public String type;
    public String path;
    public Long size;
    public String source;
    public String transport;
    public String targetIdentifier;

    public SharedBlockCandidateStruct(String wwid) {
        this.wwid = wwid;
    }

    public SharedBlockCandidateStruct(BlockDeviceStruct struct) {
        wwid = struct.wwid;
        vendor = struct.vendor;
        model = struct.model;
        wwn = struct.wwn;
        serial = struct.serial;
        hctl = struct.hctl;
        type = struct.type;
        size = struct.size;
        path = struct.path;
        source = struct.source;
        transport = struct.transport;
        targetIdentifier = struct.targetIdentifier;
    }

    public SharedBlockCandidateStruct() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SharedBlockCandidateStruct struct = (SharedBlockCandidateStruct) o;

        return wwid.equals(struct.wwid);
    }

    @Override
    public int hashCode() {
        return wwid.hashCode();
    }

    public String getWwid() {
        return wwid;
    }

    public void setWwid(String wwid) {
        this.wwid = wwid;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }
}
