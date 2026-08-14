package org.zstack.softwarePackage;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class SoftwarePackageGlobalConfig {
    public static final String CATEGORY = "softwarePackage";

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "3", description = "number of errors can be tolerated when uploading software package", type = Integer.class)
    public static GlobalConfig UPLOAD_FAILURE_TOLERANCE_COUNT = new GlobalConfig(CATEGORY, "upload.failure.tolerance.count");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "60", description = "the max duration can be tolerated when uploading software package, in seconds", type = Long.class)
    public static GlobalConfig UPLOAD_MAX_IDLE_IN_SECONDS = new GlobalConfig(CATEGORY, "upload.max.idle.duration.in.seconds");
}
