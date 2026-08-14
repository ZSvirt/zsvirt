package org.zstack.test.integration.core.config;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 */
@GlobalConfigDefinition
public class GlobalConfigForTest {
    public static final String CATEGORY = "Test";

    public static GlobalConfig TEST_GlobalConfig1_String = new GlobalConfig(CATEGORY, "TestString");
    public static GlobalConfig TEST_GlobalConfig2_Boolean = new GlobalConfig(CATEGORY, "TestBoolean");
    @GlobalConfigValidation(min = 0, max = 100)
    public static GlobalConfig TEST_GLOBALCONFIG_BORDER = new GlobalConfig(CATEGORY, "TestBorder");
}
