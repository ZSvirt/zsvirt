package org.zstack.zql.ast.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class ValueVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    public String visit(ASTNode node) {
        if (node instanceof ASTNode.ListValue) {
            List<String> values = ((ASTNode.ListValue) node).getValues().stream()
                    .filter(it->it instanceof ASTNode.PlainValue)
                    .map(it->((ASTNode.PlainValue) it).getText())
                    .collect(Collectors.toList());

            return String.format("(%s)", StringUtils.join(values, ","));
        } else if (node instanceof ASTNode.PlainValue) {
            return String.format("%s", ((ASTNode.PlainValue) node).getText());
        } else if (node instanceof ASTNode.ComplexValue) {
            return (String) node.accept(new ComplexValueVisitor());
        } else {
            throw new CloudRuntimeException(String.format("should not be here, %s", node.getClass()));
        }
    }
}
