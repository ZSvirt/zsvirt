package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:26 2020/3/23
 */
public class GetDownloadBitsFromKVMHostProgressReply extends MessageReply {

    private long totalSize;

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
}
