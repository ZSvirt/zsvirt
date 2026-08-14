package org.zstack.header.zql;

public interface MarshalZQLASTTreeExtensionPoint {
    void marshalZQLASTTree(ASTNode.Query node);
}
