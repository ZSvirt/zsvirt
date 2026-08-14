package org.zstack.storage.primary.nfs;

import org.zstack.header.message.MessageReply;

import java.util.Map;

/**
 * Created by GuoYi on 10/19/17.
 */
public class NfsToNfsMigrateBitsReply extends MessageReply {
    private Map<String, Long> dstFilesActualSize;

    public Map<String, Long> getDstFilesActualSize() {
        return dstFilesActualSize;
    }

    public void setDstFilesActualSize(Map<String, Long> dstFilesActualSize) {
        this.dstFilesActualSize = dstFilesActualSize;
    }
}
