package org.zstack.header.host;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class UploadFileToHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String taskUuid;
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    private String installPath;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public boolean isDirectUpload() {
        String directUploadScheme = "upload://";
        return url != null && url.regionMatches(true, 0, directUploadScheme, 0, directUploadScheme.length());
    }
}
