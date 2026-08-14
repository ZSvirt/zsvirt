package org.zstack.zql.ast.visitors.plugin;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.zql.ASTNode;

import static org.zstack.core.Platform.argerr;

import java.util.List;
import java.util.stream.Collectors;

public class SumPlugin extends AbstractQueryVisitorPlugin {
    public SumPlugin() {
    }

    public SumPlugin(ASTNode.Query node) {
        super(node);
    }

    @Override
    public ClauseType getClauseType() {
        return ClauseType.SUM;
    }

    @Override
    public String selectTarget() {
        ASTNode.Sum sum = (ASTNode.Sum) node;

        List<String> fields = targetFields();
        if (fields.isEmpty()) {
            throw new OperationFailureException(argerr("the field to sum must be specified"));
        }

        String sumFields = StringUtils.join(fields.stream().map(f->String.format("SUM(%s.%s) as %s", entityAlias, f, f)).collect(Collectors.toList()), ",");
        return String.format("%s,%s", sum.getGroupByField(), sumFields);
    }

    @Override
    public String groupBy() {
        ASTNode.Sum sum = (ASTNode.Sum) node;
        return String.format("GROUP BY %s.%s", entityAlias, sum.getGroupByField());
    }
}
