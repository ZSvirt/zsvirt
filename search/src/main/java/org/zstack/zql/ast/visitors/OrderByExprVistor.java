package org.zstack.zql.ast.visitors;

import org.zstack.core.Platform;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

/**
 * Created by MaJin on 2020/4/29.
 */
public class OrderByExprVistor implements ASTVisitor<String, ASTNode.OrderByExpr> {
    @Override
    public String visit(ASTNode.OrderByExpr node) {
        if (!node.getDirection().equalsIgnoreCase("asc") && !node.getDirection().equalsIgnoreCase("desc")) {
            throw new ZQLError(Platform.i18n("invalid order by clause, expect direction[asc,desc] but got %s", node.getDirection()));
        }

        return String.format("%s %s", node.getTarget().getText(), node.getDirection().toUpperCase());
    }
}
