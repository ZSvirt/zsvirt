package org.zstack.storage.volume;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/7/20.
 */
@GlobalPropertyDefinition
public class VolumeGlobalProperty {
    @GlobalProperty(name="rootVolumeFindMissingImageUuid", defaultValue = "false")
    public static boolean ROOT_VOLUME_FIND_MISSING_IMAGE_UUID;
    @GlobalProperty(name="syncVolumeSize", defaultValue = "false")
    public static boolean SYNC_VOLUME_SIZE;
}
