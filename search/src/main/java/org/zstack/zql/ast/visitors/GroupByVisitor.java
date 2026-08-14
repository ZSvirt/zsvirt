package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class GroupByVisitor implements ASTVisitor<String, ASTNode.GroupByExpr> {
    @Override
    public String visit(ASTNode.GroupByExpr node) {
        List<String> fs = node.getFields().stream().map(String::trim).collect(Collectors.toList());

        return String.format("GROUP BY %s", StringUtils.join(fs, ","));
    }
}
