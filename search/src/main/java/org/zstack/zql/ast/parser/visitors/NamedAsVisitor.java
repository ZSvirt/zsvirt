package org.zstack.zql.ast.parser.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class NamedAsVisitor extends ZQLBaseVisitor<String> {
    @Override public String visitNamedAs(ZQLParser.NamedAsContext ctx) {
        String name = ctx.namedAsValue().getText();
        name = StringUtils.removeStart(name, "'");
        name = StringUtils.removeStart(name, "\"");
        name = StringUtils.removeEnd(name, "\"");
        name = StringUtils.removeEnd(name, "'");
        return name;
    }
}
