package org.zstack.header.zql;

public interface RestrictByExprExtensionPoint {
    class SkipThisRestrictExprException extends RuntimeException {
    }

    String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr);
}
