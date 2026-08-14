package org.zstack.baremetal.instance;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by GuoYi on 7/6/18.
 */
@GlobalConfigDefinition
public class BaremetalInstanceGlobalConfig {
    public static final String CATEGORY = "baremetalInstance";

    @GlobalConfigValidation(validValues = {"Direct", "Delay"})
    public static GlobalConfig BM_DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig BM_EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig BM_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");
}
