package org.zstack.softwarePackage.entity;

import org.zstack.core.Platform;
import org.zstack.header.longjob.LongJobMessageData;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageMsg;
import org.zstack.softwarePackage.message.UploadSoftwarePackageMsg;

public class UploadSoftwarePackageLongJobData extends LongJobMessageData {
    public String resourceUuid;
    public String name;
    public String managementNodeUuid;
    public String hostUuid;
    public String url;
    public String installPath;
    public String type;

    public UploadSoftwarePackageLongJobData(NeedReplyMessage msg) {
        super(msg);
    }

    public boolean needTrack() {
        return url != null && url.startsWith("upload://");
    }

    public static UploadSoftwarePackageLongJobData buildFileLongJobDataFromMsg(UploadSoftwarePackageMsg msg) {
        UploadSoftwarePackageLongJobData data = new UploadSoftwarePackageLongJobData(msg);
        data.resourceUuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        data.name = msg.getName() != null ? msg.getName() : "softwarePackage";
        data.managementNodeUuid = msg.getManagementNodeUuid();
        data.hostUuid = msg.getHostUuid();
        data.url = msg.getUrl();
        data.installPath = msg.getInstallPath();
        data.type = msg.getType();
        return data;
    }

    public static UploadSoftwarePackageLongJobData buildFileLongJobDataFromApiMsg(APIUploadSoftwarePackageMsg msg) {
        UploadSoftwarePackageLongJobData data = new UploadSoftwarePackageLongJobData(msg);
        data.resourceUuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        data.name = msg.getName() != null ? msg.getName() : "softwarePackage";
        data.managementNodeUuid = msg.getManagementNodeUuid();
        data.hostUuid = msg.getHostUuid();
        data.url = msg.getUrl();
        data.installPath = msg.getInstallPath();
        data.type = msg.getType();
        return data;
    }
}
