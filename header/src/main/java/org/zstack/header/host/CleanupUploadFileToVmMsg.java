package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class CleanupUploadFileToVmMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String taskUuid;

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
}
