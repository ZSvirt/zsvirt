package org.zstack.tag2;

import org.zstack.header.tag.UserTagInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.Account;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;


public class Tag2ZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String PATTERN_OWNER_NAME = "__PATTERN_OWNER_NAME__";
    private static final String PATTERN_OWNER_FIELD = "__PATTERN_OWNER_FIELD__";

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        if (UserTagInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(PATTERN_OWNER_NAME);
            expr.setField(PATTERN_OWNER_FIELD);
            node.addRestrictExpr(expr);
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (PATTERN_OWNER_FIELD.equals(expr.getField()) && PATTERN_OWNER_NAME.equals(expr.getEntity())) {
            return filterPatternOwner(context);
        }

        return null;
    }

    private String filterPatternOwner(ZQLExtensionContext context) {
        if (Account.isAdminPermission(context.getAPISession())) {
            throw new SkipThisRestrictExprException();
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        return String.format("(%s.uuid in (select tag.uuid from UserTagVO tag, AccountResourceRefVO ref" +
                        " where tag.tagPatternUuid = ref.resourceUuid" +
                        " and ref.accountUuid = '%s' and ref.type = 'Own')" +
                        " or %s.tagPatternUuid is null)",
                src.simpleInventoryName(), context.getAPISession().getAccountUuid(), src.simpleInventoryName());
    }
}
