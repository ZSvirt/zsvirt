package org.zstack.header.host;

import java.util.List;

public class HostNUMANode {
    public List<String> distance;
    public List<String> cpus;
    public Long free;
    public Long size;
    public String nodeID;
    public List<String> VMsUuid;

    public String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    public List<String> getDistance() {
        return distance;
    }

    public void setDistance(List<String> distance) {
        this.distance = distance;
    }

    public List<String> getCpus() {
        return cpus;
    }

    public void setCpus(List<String> cpus) {
        this.cpus = cpus;
    }

    public Long getFree() {
        return free;
    }

    public void setFree(Long free) {
        this.free = free;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<String> getVMsUuid() {
        return VMsUuid;
    }

    public void setVMsUuid(List<String> VMsUuid) {
        this.VMsUuid = VMsUuid;
    }
}
