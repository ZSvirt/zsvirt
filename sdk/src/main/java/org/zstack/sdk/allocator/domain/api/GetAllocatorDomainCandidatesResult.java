package org.zstack.sdk.allocator.domain.api;



public class GetAllocatorDomainCandidatesResult {
    public java.util.List hosts;
    public void setHosts(java.util.List hosts) {
        this.hosts = hosts;
    }
    public java.util.List getHosts() {
        return this.hosts;
    }

    public java.util.List clusters;
    public void setClusters(java.util.List clusters) {
        this.clusters = clusters;
    }
    public java.util.List getClusters() {
        return this.clusters;
    }

    public java.util.List primaryStorages;
    public void setPrimaryStorages(java.util.List primaryStorages) {
        this.primaryStorages = primaryStorages;
    }
    public java.util.List getPrimaryStorages() {
        return this.primaryStorages;
    }

    public java.util.List l3Networks;
    public void setL3Networks(java.util.List l3Networks) {
        this.l3Networks = l3Networks;
    }
    public java.util.List getL3Networks() {
        return this.l3Networks;
    }

}
