package org.zstack.sdk;



public class ShrinkResult  {

    public long oldSize;
    public void setOldSize(long oldSize) {
        this.oldSize = oldSize;
    }
    public long getOldSize() {
        return this.oldSize;
    }

    public long size;
    public void setSize(long size) {
        this.size = size;
    }
    public long getSize() {
        return this.size;
    }

    public long deltaSize;
    public void setDeltaSize(long deltaSize) {
        this.deltaSize = deltaSize;
    }
    public long getDeltaSize() {
        return this.deltaSize;
    }

}
