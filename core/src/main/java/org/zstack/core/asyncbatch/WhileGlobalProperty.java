package org.zstack.core.asyncbatch;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class WhileGlobalProperty {
    @GlobalProperty(name="While.allMode.concurrencyLevel", defaultValue = "30")
    public static int CONCURRENCY_LEVEL_OF_ALL_MODE;
}
