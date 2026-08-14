package org.zstack.tag;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.UserTagVO;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.HashMap;
import java.util.Map;

public class TagZQLExtension implements RestrictByExprExtensionPoint {
    public static final String USER_TAG_NAME = "__userTag__";
    public static final String SYS_TAG_NAME = "__systemTag__";

    private static Map<String, String> FALSE_OP = new HashMap();

    static {
        FALSE_OP.put("!=", "=");
        FALSE_OP.put("not in", "in");
        FALSE_OP.put("not like", "like");
    }

    private String getConditionValue(String op, ASTNode.RestrictExpr expr) {
        if ("in".equals(op) || "not in".equals(op)) {
            ASTNode.ListValue lv = (ASTNode.ListValue) expr.getValue();
            return String.format("(%s)", StringUtils.join(lv.getValues(), ","));
        } else {
            return ((ASTNode.PlainValue)expr.getValue()).getText();
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (expr.getEntity() != null) {
            return null;
        }

        if (!expr.getField().equals(USER_TAG_NAME) && !expr.getField().equals(SYS_TAG_NAME)) {
            return null;
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());

        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();
        String tableName = expr.getField().equals(USER_TAG_NAME) ? UserTagVO.class.getSimpleName() : SystemTagVO.class.getSimpleName();

        String subCondition;
        if (FALSE_OP.containsKey(expr.getOperator())) {
            String reserveOp = FALSE_OP.get(expr.getOperator());
            subCondition = String.format("tagvo.uuid IN (SELECT tagvo_.uuid FROM %s tagvo_ WHERE tagvo_.tag %s %s)",
                    tableName, reserveOp, getConditionValue(reserveOp, expr));

            return String.format("(%s.%s NOT IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                    src.simpleInventoryName(), primaryKey, tableName, subCondition);
        } else {
            if (expr.getValue() == null) {
                subCondition = String.format("tagvo.tag %s", expr.getOperator());
            } else {
                subCondition = String.format("tagvo.tag %s %s", expr.getOperator(), getConditionValue(expr.getOperator(), expr));
            }

            return String.format("(%s.%s IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                    src.simpleInventoryName(), primaryKey, tableName, subCondition);
        }
    }
}
