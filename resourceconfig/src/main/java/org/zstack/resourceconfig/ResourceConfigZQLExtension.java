package org.zstack.resourceconfig;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.Account;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;


public class ResourceConfigZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String RESOURCE_CONFIG_OWNER_NAME = "__RESOURCE_CONFIG_OWNER_NAME__";
    private static final String RESOURCE_CONFIG_OWNER_FIELD = "__RESOURCE_CONFIG_OWNER_FIELD__";

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        if (ResourceConfigInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(RESOURCE_CONFIG_OWNER_NAME);
            expr.setField(RESOURCE_CONFIG_OWNER_FIELD);
            node.addRestrictExpr(expr);
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (RESOURCE_CONFIG_OWNER_FIELD.equals(expr.getField()) && RESOURCE_CONFIG_OWNER_NAME.equals(expr.getEntity())) {
            return filterResourceOwner(context);
        }

        return null;
    }

    protected String filterResourceOwner(ZQLExtensionContext context) {
        if (Account.isAdminPermission(context.getAPISession())) {
            throw new SkipThisRestrictExprException();
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        return String.format("%s.uuid in (select config.uuid from ResourceConfigVO config, AccountResourceRefVO ref" +
                        " where config.resourceUuid = ref.resourceUuid" +
                        " and ref.accountUuid = '%s' and ref.type = 'Own')",
                src.simpleInventoryName(), context.getAPISession().getAccountUuid());
    }
}
