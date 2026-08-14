package org.zstack.expon;

import org.zstack.expon.sdk.uss.UssGatewayModule;

public class ExponNameHelper {

    public static String buildVhostControllerName(String zsVolumeUuid) {
        return "volume-" + zsVolumeUuid;
    }

    public static String buildVhostControllerPath(String zsVolumeUuid) {
        return "/var/run/wds/volume-" + zsVolumeUuid;
    }

    public static String getVolumeUuidFromVhostControllerPath(String path) {
        return path.replace("/var/run/wds/volume-", "");
    }

    public static String buildUssGwName(String protocol, String managerIp) {
        return protocol + "_" + managerIp.replace(".", "_");
    }

    public static String getUssManagerIp(String name) {
        return name.split("_", 2)[1].replace("_", ".");
    }

    public static String buildExponPath(String poolName, String volId) {
        String base = volId.replace("-", "");
        return String.format("expon://%s/%s", poolName, base);
    }

    public static String buildExponSnapshotPath(String volPath, String snapId) {
        return volPath + "@" + snapId.replace("-", "");
    }

    public static String getPoolNameFromPath(String url) {
        return url.replace("expon://", "").split("/")[0];
    }

    public static String getVolIdFromPath(String url) {
        String volId = url.replace("expon://", "").split("/")[1].split("@")[0];
        return getExponId(volId);
    }

    public static String getSnapIdFromPath(String url) {
        String snapId = url.replace("expon://", "").split("/")[1].split("@")[1];
        return getExponId(snapId);
    }

    public static String getExponId(String uuid) {
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20);
    }

    /**
     * @see org.zstack.expon.sdk.pool.BlacklistModule
     */
    public static String buildExponVolumeBoundPath(UssGatewayModule uss, String volumeName) {
        return String.format("%s:%s/%s/VOLUME/%s",
                uss.getBusinessNetwork().split("/")[0],
                uss.getBusinessPort(),
                uss.getUssId(),
                volumeName);
    }

    public static String getVolumeNameFromBoundPath(String path) {
        return path.split("/")[3];
    }
}
