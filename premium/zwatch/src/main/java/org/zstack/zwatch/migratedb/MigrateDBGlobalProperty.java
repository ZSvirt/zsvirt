package org.zstack.zwatch.migratedb;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class MigrateDBGlobalProperty {
    @GlobalProperty(name = "MigrateDB.write.normalevents", defaultValue = "false")
    public static boolean MIGRATEDB_WRITE_NORMAL_EVENTS;
}
