package org.zstack.storage.primary.block;

import org.zstack.header.configuration.PythonClass;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/2 16:28
 */
public class BlockPrimaryStorageConstants {
    public static final String SERVICE_ID = "blockStorage";

    public static final String HEART_BEAT_PATH_PREFIX = "/opt/zstack/block/heart/";

    @PythonClass
    public static final String BLOCK_PRIMARY_STORAGE_TYPE = "BlockStorage";

    public static final String BLOCK_INSTALL_PATH_SCHEME = "block://";

    public static final String BLOCK_PRIMARY_STORAGE_HEARTBEAT_LUN_NAME_PREFIX = "primary-heartbeat-";

    public static final String BLOCK_VOLUME_LUN_NAME_PREFIX = "cloud-volume-";

    public static final String BLOCK_IMAGE_CACHE_LUN_NAME_PREFIX = "cloud-image-cache-";

    public static final String BLOCK_SNAPSHOT_LUN_NAME_PREFIX = "cloud-snapshot-";

    enum Params {
        BlockPrimaryStorageHostRef,
        BlockPrimaryStorageBackend,
        BlockScsiLun,
        TemplateLun,
        VolumeProvisioningStrategy,
        CheckBeforeDeleteLunMap
    }
}