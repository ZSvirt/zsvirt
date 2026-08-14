package org.zstack.storage.ceph;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 8/5/2015.
 */
@GlobalConfigDefinition
public class CephGlobalConfig {
    public static final String CATEGORY = "ceph";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig IMAGE_CACHE_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "imageCache.cleanup.interval");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_DELETE_POOL = new GlobalConfig(CATEGORY, "primaryStorage.deletePool");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PRIMARY_STORAGE_MON_RECONNECT_DELAY = new GlobalConfig(CATEGORY, "primaryStorage.mon.reconnectDelay");
    @GlobalConfigValidation
    public static GlobalConfig PRIMARY_STORAGE_MON_AUTO_RECONNECT = new GlobalConfig(CATEGORY, "primaryStorage.mon.autoReconnect");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig BACKUP_STORAGE_MON_RECONNECT_DELAY = new GlobalConfig(CATEGORY, "backupStorage.mon.reconnectDelay");
    @GlobalConfigValidation
    public static GlobalConfig BACKUP_STORAGE_MON_AUTO_RECONNECT = new GlobalConfig(CATEGORY, "backupStorage.mon.autoReconnect");
    @GlobalConfigValidation
    public static GlobalConfig GC_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig TRASH_CLEANUP_INTERVAL = new GlobalConfig(CATEGORY, "trash.cleanup.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig IMAGE_EXPIRATION_TIME = new GlobalConfig(CATEGORY, "image.expiration.time");
    @GlobalConfigValidation
    public static GlobalConfig CEPH_BS_ALLOW_PORTS = new GlobalConfig(CATEGORY, "cephbs.allow.ports");
    @GlobalConfigValidation
    public static GlobalConfig CEPH_PS_ALLOW_PORTS = new GlobalConfig(CATEGORY, "cephps.allow.ports");

    @GlobalConfigDef(type = Boolean.class, defaultValue = "true")
    @GlobalConfigValidation
    public static GlobalConfig PREVENT_VM_SPLIT_BRAIN = new GlobalConfig(CATEGORY, "checkImageWatcherBeforeStartVm");
    @GlobalConfigValidation(min = 1, max = 128)
    public static GlobalConfig CEPH_SYNC_LEVEL = new GlobalConfig(CATEGORY, "ceph.syncLevel");

    @GlobalConfigDef(type = Integer.class, defaultValue = "30", description = "third party platform sdk timeout, in minutes")
    @GlobalConfigValidation
    public static GlobalConfig THIRD_PARTY_SDK_TIMEOUT = new GlobalConfig(CATEGORY, "thirdPartySdkTimeout");
    // for ui use only
    @GlobalConfigValidation
    public static GlobalConfig SDS_ADMIN_PASSWORD = new GlobalConfig(CATEGORY, "sds.admin.password");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PRIMARY_STORAGE_MON_MAXIMUM_PING_FAILURE = new GlobalConfig(CATEGORY, "primaryStorage.mon.ping.maxFailure");
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig SLEEP_TIME_AFTER_PING_FAILURE = new GlobalConfig(CATEGORY, "ping.sleepPeriodAfterFailure");

}
