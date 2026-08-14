package org.zstack.header.storage.snapshot;

import org.zstack.header.rest.SDK;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:29 2020/7/28
 */
@SDK
public class ShrinkResult {

    private long oldSize;

    private long size;

    private long deltaSize;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDeltaSize() {
        return deltaSize;
    }

    public void setDeltaSize(long deltaSize) {
        this.deltaSize = deltaSize;
    }

    public long getOldSize() {
        return oldSize;
    }

    public void setOldSize(long oldSize) {
        this.oldSize = oldSize;
    }
}
