package org.zstack.image;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/6/24.
 */
@GlobalPropertyDefinition
public class ImageGlobalProperty {
    @GlobalProperty(name="syncImageActualSize", defaultValue = "false")
    public static boolean SYNC_IMAGE_ACTUAL_SIZE_ON_START;
    @GlobalProperty(name="fixImageCacheUuid", defaultValue = "false")
    public static boolean FIX_IMAGE_CACHE_UUID;
}
