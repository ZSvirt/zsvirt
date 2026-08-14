package org.zstack.core.db;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class DbGlobalProperty {
    @GlobalProperty(name="entityPackages", defaultValue = "org.zstack")
    public static String ENTITY_PACKAGES;
    @GlobalProperty(name="C3p0ConnectionTester.isValidTimeout", defaultValue = "60")
    public static int C3P0_IS_VALID_TIMEOUT;
}
