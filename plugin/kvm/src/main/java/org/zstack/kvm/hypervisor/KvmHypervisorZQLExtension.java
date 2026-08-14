package org.zstack.kvm.hypervisor;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.Account;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoInventory;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

/**
 * Created by Wenhao.Zhang on 23/03/09
 */
public class KvmHypervisorZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final String KVM_RESOURCE_OWNER_NAME = "__KVM_RESOURCE_OWNER_NAME__";
    private static final String KVM_RESOURCE_OWNER_FIELD = "__KVM_RESOURCE_OWNER_FIELD__";

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        if (KvmHypervisorInfoInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(KVM_RESOURCE_OWNER_NAME);
            expr.setField(KVM_RESOURCE_OWNER_FIELD);
            node.addRestrictExpr(expr);
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (KVM_RESOURCE_OWNER_FIELD.equals(expr.getField()) && KVM_RESOURCE_OWNER_NAME.equals(expr.getEntity())) {
            return filterResourceOwner(context);
        }

        return null;
    }

    protected String filterResourceOwner(ZQLExtensionContext context) {
        if (Account.isAdminPermission(context.getAPISession())) {
            throw new SkipThisRestrictExprException();
        }

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        return String.format("%s.uuid in (select info.uuid from KvmHypervisorInfoVO info, AccountResourceRefVO ref" +
                        " where info.uuid = ref.resourceUuid" +
                        " and ref.accountUuid = '%s' and ref.type = 'Own')",
                src.simpleInventoryName(), context.getAPISession().getAccountUuid());
    }
}
