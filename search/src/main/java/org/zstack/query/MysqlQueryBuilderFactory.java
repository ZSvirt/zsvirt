package org.zstack.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.query.QueryBuilder;
import org.zstack.header.query.QueryBuilderFactory;
import org.zstack.header.query.QueryBuilderType;

public class MysqlQueryBuilderFactory implements QueryBuilderFactory {
    public static QueryBuilderType type = new QueryBuilderType("Mysql");
    
    @Autowired
    private MysqlQueryBuilderImpl3 builder;
    
    @Override
    public QueryBuilderType getQueryBuilderType() {
        return type;
    }

    @Override
    public QueryBuilder createQueryBuilder() {
        return builder;
    }

}
