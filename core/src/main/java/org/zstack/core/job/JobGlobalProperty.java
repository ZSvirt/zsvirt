package org.zstack.core.job;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class JobGlobalProperty {
    @GlobalProperty(name = "QuartzJdbcJobFacade.threadCount", defaultValue = "50")
    public static int QUARTZ_THREAD_COUNT;
    @GlobalProperty(name = "QuartzJdbcJobFacade.jdbcUrl")
    public static String QUARTZ_DB_URL;
    @GlobalProperty(name = "QuartzJdbcJobFacade.dbUser")
    public static String QUARTZ_DB_USER;
    @GlobalProperty(name = "QuartzJdbcJobFacade.dbPassword")
    public static String QUARTZ_DB_PASSWORD;
    @GlobalProperty(name = "QuartzJdbcJobFacade.maxDbConnection", defaultValue = "10")
    public static int QUARTZ_DB_MAX_CONNECTIONS;
}
