package org.zstack.header.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QueryBuilderType {
    private static Map<String, QueryBuilderType> types = Collections.synchronizedMap(new HashMap<String, QueryBuilderType>());
    private final String typeName;

    public QueryBuilderType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static QueryBuilderType valueOf(String typeName) {
        QueryBuilderType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("QueryBuilder type: " + typeName + " was not registered by any HypervisorFactory");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof QueryBuilderType)) {
            return false;
        }

        QueryBuilderType type = (QueryBuilderType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        return types.keySet();
    }
}
