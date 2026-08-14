package org.zstack.core.db.hibernate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseUpdateHandlerImpl;

/**
 * Created by lining on 2020/4/17.
 */
public class InlineIdsOrClauseBulkIdStrategy2 extends InlineIdsOrClauseBulkIdStrategy {
    @Override
    public DeleteHandler buildDeleteHandler(
            SessionFactoryImplementor factory,
            HqlSqlWalker walker) {
        return new InlineIdsIdsOrClauseDeleteHandlerImpl2( factory, walker );
    }

    @Override
    public UpdateHandler buildUpdateHandler(
            SessionFactoryImplementor factory,
            HqlSqlWalker walker) {
        return new InlineIdsOrClauseUpdateHandlerImpl2( factory, walker );
    }
}
