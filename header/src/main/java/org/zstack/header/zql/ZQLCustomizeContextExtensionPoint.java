package org.zstack.header.zql;

public interface ZQLCustomizeContextExtensionPoint {
    Runnable zqlCustomizeContext(ASTNode.Query node);
}
