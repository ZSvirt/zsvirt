package org.zstack.zwatch.migratedb;

import org.zstack.header.identity.AccountConstant;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.Account;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

/**
 * Created by yaoning on 2021/6/16.
 */
public class ZWatchAlertZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String ZWATCH_ALERT_OWNER_NAME = "__ZWATCH_ALERT_OWNER_NAME__";
    private static final String ZWATCH_ALERT_OWNER_FIELD = "__ZWATCH_ALERT_OWNER_FIELD__";

    private static final String ZWATCH_AUDIT_OWNER_NAME = "__ZWATCH_AUDIT_OWNER_NAME__";
    private static final String ZWATCH_AUDIT_OWNER_FIELD = "__ZWATCH_AUDIT_OWNER_FIELD__";


    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        if (AlarmRecordsInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(ZWATCH_ALERT_OWNER_NAME);
            expr.setField(ZWATCH_ALERT_OWNER_FIELD);
            node.addRestrictExpr(expr);
        } else if (EventRecordsInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(ZWATCH_ALERT_OWNER_NAME);
            expr.setField(ZWATCH_ALERT_OWNER_FIELD);
            node.addRestrictExpr(expr);
        } else if (AuditsInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(ZWATCH_AUDIT_OWNER_NAME);
            expr.setField(ZWATCH_AUDIT_OWNER_FIELD);
            node.addRestrictExpr(expr);
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (ZWATCH_ALERT_OWNER_FIELD.equals(expr.getField()) &&
                ZWATCH_ALERT_OWNER_NAME.equals(expr.getEntity())) {
            return filterZWatchAlert(context);
        }

        if (ZWATCH_AUDIT_OWNER_FIELD.equals(expr.getField()) &&
                ZWATCH_AUDIT_OWNER_NAME.equals(expr.getEntity())) {
            return filterZWatchAudit(context);
        }

        return null;
    }

    private String filterZWatchAlert(ZQLExtensionContext context) {
        if (Account.supportToQueryEventsFromAllAccounts(context.getAPISession())) {
            throw new SkipThisRestrictExprException();
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        return String.format("(%s.accountUuid = '%s')", src.simpleInventoryName(),
                context.getAPISession().getAccountUuid());
    }

    private String filterZWatchAudit(ZQLExtensionContext context) {
        if (Account.supportToQueryAuditsFromAllAccounts(context.getAPISession())) {
            throw new SkipThisRestrictExprException();
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        return String.format("(%s.operatorAccountUuid = '%s')", src.simpleInventoryName(),
                context.getAPISession().getAccountUuid());
    }
}
