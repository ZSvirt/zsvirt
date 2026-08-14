package org.zstack.zmigrate.compute;

import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.zmigrate.compute.ZMigrateUtils.findZMigrateVmUuids;
import static org.zstack.zmigrate.ZMigrateSystemTags.*;

public class ZMigrateZQLExtension implements MarshalZQLASTTreeExtensionPoint, RestrictByExprExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ZMigrateZQLExtension.class);
    private static final String ENTITY_NAME = "__ZMIGRATE_VM_FILTER__";
    private static final String ENTITY_FIELD = "__ZMIGRATE_VM_FILTER_FIELD__";

    @Override
    public void marshalZQLASTTree(ASTNode.Query node) {
        if (VmInstanceInventory.class.getName().equals(ZQLContext.getQueryTargetInventoryName())) {
            ASTNode.RestrictExpr expr = new ASTNode.RestrictExpr();
            expr.setEntity(ENTITY_NAME);
            expr.setField(ENTITY_FIELD);
            node.addRestrictExpr(expr);
        }
    }

    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (!ENTITY_NAME.equals(expr.getEntity()) || !ENTITY_FIELD.equals(expr.getField())) {
            return null;
        }

        return filterZMigrateVm(context);
    }

    private String filterZMigrateVm(ZQLExtensionContext context) {
        List<String> vmUuids = collectZMigrateVmUuids();
        if (vmUuids.isEmpty()) {
            throw new SkipThisRestrictExprException();
        }

        logger.debug(String.format("filtering %d ZMigrate VMs from ZQL query: %s", vmUuids.size(), vmUuids));

        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        String alias = src.simpleInventoryName();
        String inList = vmUuids.stream()
                .filter(u -> u.matches("^[0-9a-fA-F]{32}$"))
                .map(u -> "'" + u + "'")
                .collect(Collectors.joining(","));
        if (inList.isEmpty()) {
            throw new SkipThisRestrictExprException();
        }
        return String.format("(%s.uuid NOT IN (%s))", alias, inList);
    }

    private List<String> collectZMigrateVmUuids() {
        List<String> vmUuids = new ArrayList<>();
        vmUuids.addAll(findZMigrateVmUuids(ZMIGRATE_MANAGEMENT));
        vmUuids.addAll(findZMigrateVmUuids(ZMIGRATE_GATEWAY));
        return vmUuids;
    }
}
