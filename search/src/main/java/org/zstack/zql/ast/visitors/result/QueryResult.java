package org.zstack.zql.ast.visitors.result;

import org.zstack.zql.ast.ZQLMetadata;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.function.Function;

public class QueryResult {
    public String sql;
    public List<String> targetFieldNames;
    public Function<EntityManager, Query> createJPAQuery;
    public Function<EntityManager, Query> createSimpleCountQuery;
    public Function<EntityManager, Query> createCountQuery;
    public ZQLMetadata.InventoryMetadata inventoryMetadata;
    public List<ReturnWithResult> returnWith;
    public List<FilterByResult> filterBy;
}
