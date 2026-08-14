package org.zstack.expon;

public class ExponIscsiHelper {

    static String iscsiTargetPrefix = "iscsi_zstack_active";

    static String iscsiTargetImagePrefix = "iscsi_zstack_image_active";

    static String iscsiExportTargetName = "iscsi_zstack_export";

    static String iscsiHeartbeatTargetName = "iscsi_zstack_heartbeat";

    static String iscsiHeartbeatClientName = "iscsi_zstack_heartbeat";

    static String iscsiHeartbeatVolumeName = "iscsi_zstack_heartbeat";

    static String iscsiPrefix = "iscsi_zstack";

    static String buildIscsiExportClientName(String clientIp) {
        return "iscsi_" + clientIp.replace(".", "_");
    }

    public static String buildIscsiVolumeClientName(String volumeUuid) {
        return "volume_" + volumeUuid;
    }

    static String buildVolumeIscsiTargetName(int index) {
        return String.format("%s_%d", iscsiTargetPrefix, index);
    }

    static String buildImageIscsiTargetName(int index) {
        return String.format("%s_%d", iscsiTargetImagePrefix, index);
    }

    static int getIndexFromIscsiTargetName(String name) {
        return Integer.parseInt(name.substring(name.lastIndexOf("_") + 1));
    }
}
