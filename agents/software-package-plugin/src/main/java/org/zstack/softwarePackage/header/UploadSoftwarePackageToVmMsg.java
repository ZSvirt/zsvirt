package org.zstack.softwarePackage.header;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class UploadSoftwarePackageToVmMsg extends NeedReplyMessage {
    private String type;
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    private String vmInstanceUuid;
    private String hostUuid;
    private String targetIp;
    private String uploadTaskUuid;
    private String ownerSessionUuid;
    private String cancellationApiId;
    private boolean deferCleanupToLongJob;

    public boolean needTrack() {
        return url != null && url.regionMatches(true, 0, "upload://", 0, "upload://".length());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getUploadTaskUuid() {
        return uploadTaskUuid;
    }

    public void setUploadTaskUuid(String uploadTaskUuid) {
        this.uploadTaskUuid = uploadTaskUuid;
    }

    public String getOwnerSessionUuid() {
        return ownerSessionUuid;
    }

    public void setOwnerSessionUuid(String ownerSessionUuid) {
        this.ownerSessionUuid = ownerSessionUuid;
    }

    public String getCancellationApiId() {
        return cancellationApiId;
    }

    public void setCancellationApiId(String cancellationApiId) {
        this.cancellationApiId = cancellationApiId;
    }

    public boolean isDeferCleanupToLongJob() {
        return deferCleanupToLongJob;
    }

    public void setDeferCleanupToLongJob(boolean deferCleanupToLongJob) {
        this.deferCleanupToLongJob = deferCleanupToLongJob;
    }

    public static UploadSoftwarePackageToVmMsg fromApiMessage(
            APIUploadSoftwarePackageToVmMsg apiMessage, String uploadTaskUuid) {
        if (apiMessage == null) {
            throw new IllegalArgumentException("apiMessage cannot be null");
        }

        UploadSoftwarePackageToVmMsg msg = new UploadSoftwarePackageToVmMsg();
        msg.setType(apiMessage.getType());
        msg.setUrl(apiMessage.getUrl());
        msg.setVmInstanceUuid(apiMessage.getVmInstanceUuid());
        msg.setHostUuid(apiMessage.getHostUuid());
        msg.setTargetIp(apiMessage.getTargetIp());
        msg.setUploadTaskUuid(uploadTaskUuid);
        return msg;
    }
}
