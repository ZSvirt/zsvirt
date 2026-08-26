package org.zstack.header.query;

public interface QueryBuilderFactory {
    QueryBuilderType getQueryBuilderType();

    QueryBuilder createQueryBuilder();
}
