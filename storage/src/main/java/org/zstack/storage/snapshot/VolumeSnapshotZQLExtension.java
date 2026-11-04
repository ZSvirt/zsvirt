package org.zstack.storage.snapshot;

import org.zstack.core.db.EntityMetadata;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.identity.IdentityZQLExtension;
import org.zstack.zql.ast.ZQLMetadata;

/** Extend the existing account restriction only for groups and their member snapshots. */
public class VolumeSnapshotZQLExtension extends IdentityZQLExtension {
    @Override
    public String restrictByExpr(ZQLExtensionContext context, ASTNode.RestrictExpr expr) {
        if (!ENTITY_NAME.equals(expr.getEntity()) || !ENTITY_FIELD.equals(expr.getField())) {
            return null;
        }

        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.getInventoryMetadataByName(context.getQueryTargetInventoryName());
        Class<?> resourceType = inventory.inventoryAnnotation.mappingVOClass();
        if (resourceType != VolumeSnapshotGroupVO.class && resourceType != VolumeSnapshotVO.class) {
            return null;
        }

        String resourceFilter = super.restrictByExpr(context, expr);
        String primaryKey = EntityMetadata.getPrimaryKeyField(resourceType).getName();
        return VolumeSnapshotQueryAccess.includeOwnedVmSnapshots(resourceType, inventory.simpleInventoryName(),
                primaryKey, context.getAPISession().getAccountUuid(), resourceFilter);
    }
}
