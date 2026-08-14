package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

/**
 * Created by MaJin on 2019/6/3.
 */
public class DistinctVistor implements ASTVisitor<String, ASTNode.Distinct> {
    @Override
    public String visit(ASTNode.Distinct node) {
        return "DISTINCT %s";
    }
}