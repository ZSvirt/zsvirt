package org.zstack.storage.volume;

import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class MevocoVolumeGlobalConfig {
    public static final String CATEGORY = "premiumVolume";

    @GlobalConfigValidation(inNumberRange = {0, 128})
    @GlobalConfigDef(defaultValue = "0", type = Integer.class, description = "the multiQueues of data volume.")
    @BindResourceConfig({VolumeVO.class, ClusterVO.class})
    public static PremiumGlobalConfig VOLUME_MULTI_QUEUES = new PremiumGlobalConfig(CATEGORY, "multiQueues.volume");

}
