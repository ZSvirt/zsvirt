package org.zstack.storage.addon.primary;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class ExternalPrimaryStorageGlobalConfig {
    public static final String CATEGORY = "externalPrimaryStorage";

    @GlobalConfigValidation(validValues = {"iSCSI", "NVMEoF", "CBD"})
    @GlobalConfigDef(defaultValue = "iSCSI", description = "image export protocol of external primary storage")
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig IMAGE_EXPORT_PROTOCOL = new GlobalConfig(CATEGORY, "image.export.protocol");
}