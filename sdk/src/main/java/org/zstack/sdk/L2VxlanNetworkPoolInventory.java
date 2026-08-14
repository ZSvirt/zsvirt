package org.zstack.sdk;



public class L2VxlanNetworkPoolInventory extends org.zstack.sdk.L2NetworkInventory {

    public java.util.List attachedVtepRefs;
    public void setAttachedVtepRefs(java.util.List attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }
    public java.util.List getAttachedVtepRefs() {
        return this.attachedVtepRefs;
    }

    public java.util.List remoteVteps;
    public void setRemoteVteps(java.util.List remoteVteps) {
        this.remoteVteps = remoteVteps;
    }
    public java.util.List getRemoteVteps() {
        return this.remoteVteps;
    }

    public java.util.List attachedVxlanNetworkRefs;
    public void setAttachedVxlanNetworkRefs(java.util.List attachedVxlanNetworkRefs) {
        this.attachedVxlanNetworkRefs = attachedVxlanNetworkRefs;
    }
    public java.util.List getAttachedVxlanNetworkRefs() {
        return this.attachedVxlanNetworkRefs;
    }

    public java.util.List attachedVniRanges;
    public void setAttachedVniRanges(java.util.List attachedVniRanges) {
        this.attachedVniRanges = attachedVniRanges;
    }
    public java.util.List getAttachedVniRanges() {
        return this.attachedVniRanges;
    }

    public java.util.Map attachedCidrs;
    public void setAttachedCidrs(java.util.Map attachedCidrs) {
        this.attachedCidrs = attachedCidrs;
    }
    public java.util.Map getAttachedCidrs() {
        return this.attachedCidrs;
    }

}
