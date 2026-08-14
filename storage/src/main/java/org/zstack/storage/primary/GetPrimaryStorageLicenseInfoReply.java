package org.zstack.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageLicenseInfo;

public class GetPrimaryStorageLicenseInfoReply extends MessageReply {
    private PrimaryStorageLicenseInfo primaryStorageLicenseInfo;

    public PrimaryStorageLicenseInfo getPrimaryStorageLicenseInfo() {
        return primaryStorageLicenseInfo;
    }

    public void setPrimaryStorageLicenseInfo(PrimaryStorageLicenseInfo primaryStorageLicenseInfo) {
        this.primaryStorageLicenseInfo = primaryStorageLicenseInfo;
    }
}
