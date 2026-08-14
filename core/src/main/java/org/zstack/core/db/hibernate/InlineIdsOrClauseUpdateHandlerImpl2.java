package org.zstack.core.db.hibernate;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.spi.id.inline.IdsClauseBuilder;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseUpdateHandlerImpl;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.sql.Update;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by lining on 2020/4/17.
 */
public class InlineIdsOrClauseUpdateHandlerImpl2 extends InlineIdsOrClauseUpdateHandlerImpl {

    public InlineIdsOrClauseUpdateHandlerImpl2(SessionFactoryImplementor factory, HqlSqlWalker walker) {
        super(factory, walker);
    }

    @Override
    public int execute(
            SharedSessionContractImplementor session,
            QueryParameters queryParameters) {

        IdsClauseBuilder values = prepareInlineStatement( session, queryParameters );

        final Map<Integer, String> currentUpdates = new LinkedHashMap<>();

        if ( !values.getIds().isEmpty() ) {

            String[] tableNames = getTargetedQueryable().getConstraintOrderedTableNameClosure();
            String[][] columnNames = getTargetedQueryable().getContraintOrderedTableKeyColumnClosure();

            String idSubselect = values.toStatement();

            ParameterSpecification[][] assignmentParameterSpecifications = new ParameterSpecification[tableNames.length][];
            for ( int tableIndex = 0; tableIndex < tableNames.length; tableIndex++ ) {
                boolean affected = false;
                final List<ParameterSpecification> parameterList = new ArrayList<>();

                Update update = generateUpdate( tableNames[tableIndex], columnNames[tableIndex], idSubselect, "bulk update" );

                final List<AssignmentSpecification> assignmentSpecifications = walker().getAssignmentSpecifications();
                for ( AssignmentSpecification assignmentSpecification : assignmentSpecifications ) {
                    if ( assignmentSpecification.affectsTable( tableNames[tableIndex] ) ) {
                        affected = true;
                        update.appendAssignmentFragment( assignmentSpecification.getSqlAssignmentFragment() );
                        if ( assignmentSpecification.getParameters() != null ) {
                            Collections.addAll( parameterList, assignmentSpecification.getParameters() );
                        }
                    }
                }
                if ( affected ) {
                    currentUpdates.put( tableIndex, update.toStatementString() );
                    assignmentParameterSpecifications[tableIndex] = parameterList.toArray( new ParameterSpecification[parameterList.size()] );
                }
            }

            // Start performing the updates
            for ( Map.Entry<Integer, String> updateEntry: currentUpdates.entrySet()) {
                int i = updateEntry.getKey();
                String update = updateEntry.getValue();

                if ( update == null) {
                    continue;
                }
                try {
                    try (PreparedStatement ps = session
                            .getJdbcCoordinator().getStatementPreparer()
                            .prepareStatement( update, false )) {
                        int position = 1; // jdbc params are 1-based
                        if ( assignmentParameterSpecifications[i] != null ) {
                            for ( int x = 0; x < assignmentParameterSpecifications[i].length; x++ ) {
                                position += assignmentParameterSpecifications[i][x]
                                        .bind( ps, queryParameters, session, position );
                            }
                        }
                        session
                                .getJdbcCoordinator().getResultSetReturn()
                                .executeUpdate( ps );
                    }
                }
                catch ( SQLException e ) {
                    throw convert(
                            e,
                            "error performing bulk update",
                            update
                    );
                }
            }
        }

        // deletes.addAll(currentUpdates);

        return values.getIds().size();
    }
}
