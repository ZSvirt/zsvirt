package org.zstack.header.zql

interface ASTVisitor<T, K extends ASTNode> {
    T visit(K node)
}
