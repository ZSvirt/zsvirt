package org.zstack.storage.volume;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by mingjian.deng on 2018/11/20.
 */
@GlobalPropertyDefinition
public class MevocoVolumeGlobalProperty {
    @GlobalProperty(name="upgradeVolumeQos", defaultValue = "false")
    public static boolean UPGRADE_VOLUME_QOS;
}
