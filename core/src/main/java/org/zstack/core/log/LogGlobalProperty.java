package org.zstack.core.log;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by lining on 2019/7/17.
 */
@GlobalPropertyDefinition
public class LogGlobalProperty {
    @GlobalProperty(name="log.management.server.retentionSizeGB", defaultValue = LogConstant.PROPERTY_LOG_RETENTION_SIZE_GB_DEFAULT_VALUE)
    public static int LOG_RETENTION_SIZE_GB;
}
