package org.zstack.sdk.allocator.domain.api;



public class GetAllocatorDomainVmsResult {
    public java.util.List currentVms;
    public void setCurrentVms(java.util.List currentVms) {
        this.currentVms = currentVms;
    }
    public java.util.List getCurrentVms() {
        return this.currentVms;
    }

    public java.util.List nextStartVms;
    public void setNextStartVms(java.util.List nextStartVms) {
        this.nextStartVms = nextStartVms;
    }
    public java.util.List getNextStartVms() {
        return this.nextStartVms;
    }

}
