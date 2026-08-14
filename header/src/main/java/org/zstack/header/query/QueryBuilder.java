package org.zstack.header.query;

import java.util.List;
import java.util.Map;

public interface QueryBuilder {
    <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass);

    long count(APIQueryMessage msg, Class inventoryClass);

    Map<String, List<String>> populateQueryableFields();
}
