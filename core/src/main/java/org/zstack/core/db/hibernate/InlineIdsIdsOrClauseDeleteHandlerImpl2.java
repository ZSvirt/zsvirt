package org.zstack.core.db.hibernate;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.inline.IdsClauseBuilder;
import org.hibernate.hql.spi.id.inline.InlineIdsIdsOrClauseDeleteHandlerImpl;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2020/4/17.
 */
public class InlineIdsIdsOrClauseDeleteHandlerImpl2 extends InlineIdsIdsOrClauseDeleteHandlerImpl {
    public InlineIdsIdsOrClauseDeleteHandlerImpl2(SessionFactoryImplementor factory, HqlSqlWalker walker) {
        super(factory, walker);
    }

    @Override
    public int execute(
            SharedSessionContractImplementor session,
            QueryParameters queryParameters) {

        IdsClauseBuilder values = prepareInlineStatement( session, queryParameters );

        List<String> currentDeletes = new ArrayList<>();

        if ( !values.getIds().isEmpty() ) {
            final String idSubselect = values.toStatement();

            for ( Type type : getTargetedQueryable().getPropertyTypes() ) {
                if ( type.isCollectionType() ) {
                    CollectionType cType = (CollectionType) type;
                    AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory().getMetamodel().collectionPersister( cType.getRole() );
                    if ( cPersister.isManyToMany() ) {
                        currentDeletes.add( generateDelete(
                                cPersister.getTableName(),
                                cPersister.getKeyColumnNames(),
                                idSubselect,
                                "bulk delete - m2m join table cleanup"
                        ).toStatementString() );
                    }
                }
            }

            String[] tableNames = getTargetedQueryable().getConstraintOrderedTableNameClosure();
            String[][] columnNames = getTargetedQueryable().getContraintOrderedTableKeyColumnClosure();
            for ( int i = 0; i < tableNames.length; i++ ) {
                // TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
                //      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
                //          the table info gotten here should really be self-contained (i.e., a class representation
                //          defining all the needed attributes), then we could then get an array of those
                currentDeletes.add( generateDelete( tableNames[i], columnNames[i], idSubselect, "bulk delete" ).toStatementString() );
            }

            // Start performing the deletes
            for ( String delete : currentDeletes ) {
                if ( delete == null) {
                    continue;
                }

                try {
                    try ( PreparedStatement ps = session
                            .getJdbcCoordinator().getStatementPreparer()
                            .prepareStatement( delete, false ) ) {
                        session
                                .getJdbcCoordinator().getResultSetReturn()
                                .executeUpdate( ps );
                    }
                }
                catch ( SQLException e ) {
                    throw convert( e, "error performing bulk delete", delete );
                }
            }

            //deletes.addAll( currentDeletes );
        }

        return values.getIds().size();
    }
}
