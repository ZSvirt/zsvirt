package org.zstack.resourceattribute;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;

public class ResourceAttributeZQLExtension implements RestrictByExprExtensionPoint {
    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        // TODO
        return null;
    }
}
