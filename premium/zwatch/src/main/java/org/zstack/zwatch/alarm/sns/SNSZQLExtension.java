package org.zstack.zwatch.alarm.sns;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

/**
 * Created by kayo on 2018/7/2.
 */
public class SNSZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String SNS_OWNER_TYPE_ENTITY_NAME = "__SNS_OWNER_TYPE_FILTER__";
    private static final String SNS_OWNER_TYPE_ENTITY_FIELD = "__SNS_OWNER_TYPE_FILTER_FIELD__";

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        if (SNSApplicationEndpointInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(SNS_OWNER_TYPE_ENTITY_NAME);
            expr.setField(SNS_OWNER_TYPE_ENTITY_FIELD);
            node.addRestrictExpr(expr);
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (SNS_OWNER_TYPE_ENTITY_FIELD.equals(expr.getField()) &&
                SNS_OWNER_TYPE_ENTITY_NAME.equals(expr.getEntity())) {
            return filterSystemSNSEndPoint(context);
        }

        return null;
    }

    private String filterSystemSNSEndPoint(ZQLExtensionContext context) {
        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        return String.format("(%s.uuid in (select vo.uuid from SNSApplicationEndpointVO vo where vo.ownerType is NULL))"
                , src.simpleInventoryName());
    }
}
