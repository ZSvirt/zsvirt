package org.zstack.softwarePackage.header;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

public class UploadSoftwarePackageToVmReply extends MessageReply {
    @NoLogging(type = NoLogging.Type.Uri)
    private String uploadUrl;

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
}
