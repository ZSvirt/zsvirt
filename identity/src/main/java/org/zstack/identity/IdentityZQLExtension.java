package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IdentityZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    protected static final String ENTITY_NAME = "__ACCOUNT_FILTER__";
    protected static final String ENTITY_FIELD = "__ACCOUNT_FILTER_FIELD__";

    public static final String SKIP_IDENTITY_FILTER = "__SKIP_IDENTITY_FILTER __";

    @Autowired
    protected AccountManager acntMgr;

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        SessionInventory session = ZQLContext.getAPISession();
        if (session == null) {
            return;
        }

        ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
        expr.setEntity(ENTITY_NAME);
        expr.setField(ENTITY_FIELD);

        node.addRestrictExpr(expr);
    }

    protected List<String> getRestrictAccountUuids(SessionInventory session) {
        return null;
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (!ENTITY_NAME.equals(expr.getEntity()) || !ENTITY_FIELD.equals(expr.getField())) {
            // not for us
            return null;
        }

        Boolean skip = (Boolean) ZQLContext.getCustomizedContext(SKIP_IDENTITY_FILTER);
        if (skip != null && skip) {
            throw new SkipThisRestrictExprException();
        }

        List<String> restrictAccountUuids = getRestrictAccountUuids(context.getAPISession());
        if (Account.isAdminPermission(context.getAPISession()) && restrictAccountUuids == null) {
            throw new SkipThisRestrictExprException();
        }

        if (restrictAccountUuids == null) {
            restrictAccountUuids = new ArrayList<>();
        }

        restrictAccountUuids.add(context.getAPISession().getAccountUuid());

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        if (!acntMgr.isResourceHavingAccountReference(src.inventoryAnnotation.mappingVOClass())) {
            throw new SkipThisRestrictExprException();
        }

        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();

        String accountStr = restrictAccountUuids.stream()
                .map(uuid -> String.format("'%s'", uuid))
                .collect(Collectors.joining(","));

        return getAccountResourceSql(src.simpleInventoryName(), primaryKey, accountStr);
    }

    protected String getAccountResourceSql(String inventoryName, String primaryKey, String accountStr) {
        return String.format("(%s.%s IN (SELECT accountresourcerefvo.resourceUuid FROM AccountResourceRefVO accountresourcerefvo WHERE" +
                        " accountresourcerefvo.accountUuid in (%s) or accountresourcerefvo.type = 'SharePublic'))",
                inventoryName, primaryKey, accountStr);
    }
}
