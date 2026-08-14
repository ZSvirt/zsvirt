package org.zstack.storage.zbs;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.data.SizeUnit;

/**
 * @author Xingwei Yu
 * @date 2024/4/9 17:50
 */
public class ZbsHelper {
    public static void configUrl(String psUuid) {
        String psUrl = ZbsConstants.ZBS_CBD_PREFIX_SCHEME + psUuid;
        if (!psUrl.equals(Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url).eq(PrimaryStorageVO_.uuid, psUuid).findValue())) {
            SQL.New(PrimaryStorageVO.class).set(PrimaryStorageVO_.url, psUrl).eq(PrimaryStorageVO_.uuid, psUuid).update();
        }
    }

    public static String buildHeartbeatVolumePath(String logicalPool) {
        return String.format("cbd:%s_physical/%s/%s", logicalPool, logicalPool, ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME);
    }

    public static String buildVolumePath(String physicalPool, String logicalPool, String volId) {
        String base = volId.replace("-", "");
        return String.format(ZbsConstants.ZBS_CBD_LUN_PATH_FORMAT, physicalPool, logicalPool, base);
    }

    public static String getVolumeFromSnapshotPath(String path) {
        return path.split("@")[0];
    }

    public static String getSizeUnit(String version) {
        return new VersionComparator(version.split("-")[0]).compare(ZbsConstants.MEGABYTE_SUPPORTED_VERSION) >= 0 ? ZbsConstants.MEGABYTE_UNIT : ZbsConstants.DEFAULT_GIGABYTE_UNIT;
    }

    public static long alignSizeTo(long size, String unit) {
        return ZbsConstants.MEGABYTE_UNIT.equals(unit) ? (long) Math.ceil(SizeUnit.BYTE.toMegaByte((double) size)) : (long) Math.ceil(SizeUnit.BYTE.toGigaByte((double) size));
    }

    public static long convertSizeToByte(long size, String unit) {
        return ZbsConstants.MEGABYTE_UNIT.equals(unit) ? SizeUnit.MEGABYTE.toByte(size) : SizeUnit.GIGABYTE.toByte(size);
    }
}
