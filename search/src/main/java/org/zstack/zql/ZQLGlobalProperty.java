package org.zstack.zql;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ZQLGlobalProperty {
    @GlobalProperty(name = "zql.errorIfNoDBGraphRelation", defaultValue = "false")
    public static boolean ERROR_IF_NO_DB_GRAPH_RELATION;
}
