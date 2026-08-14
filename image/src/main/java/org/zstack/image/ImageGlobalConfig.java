package org.zstack.image;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by frank on 11/15/2015.
 */
@GlobalConfigDefinition
public class ImageGlobalConfig {
    public static final String CATEGORY = "image";

    @GlobalConfigValidation(validValues = {"Direct", "Delay", "Never"})
    public static GlobalConfig DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig DELETION_GARBAGE_COLLECTION_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");

    @GlobalConfigValidation()
    public static GlobalConfig DOWNLOAD_LOCALPATH_BLACKLIST = new GlobalConfig(CATEGORY, "download.localPath.blacklist");

    @GlobalConfigValidation()
    public static GlobalConfig DOWNLOAD_LOCALPATH_WHITELIST = new GlobalConfig(CATEGORY, "download.localPath.whitelist");

    @GlobalConfigValidation()
    public static GlobalConfig DOWNLOAD_LOCALPATH_CUSTOMFILTER = new GlobalConfig(CATEGORY, "download.localPath.customFilter");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig UPLOAD_FAILURE_TOLERANCE_COUNT = new GlobalConfig(CATEGORY, "upload.failure.tolerance.count");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig UPLOAD_MAX_IDLE_IN_SECONDS = new GlobalConfig(CATEGORY, "upload.max.idle.duration.in.seconds");
}
