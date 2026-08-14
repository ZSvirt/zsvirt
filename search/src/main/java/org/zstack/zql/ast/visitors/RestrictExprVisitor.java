package org.zstack.zql.ast.visitors;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DBGraph;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.header.zql.RestrictByExprExtensionPoint;
import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ZQLGlobalProperty;
import org.zstack.zql.ast.ZQLMetadata;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RestrictExprVisitor implements ASTVisitor<String, ASTNode.RestrictExpr> {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public String visit(ASTNode.RestrictExpr node) {
        ZQLExtensionContext context = ZQLContext.createZQLExtensionContext();
        try {
            for (RestrictByExprExtensionPoint extp : pluginRgty.getExtensionList(RestrictByExprExtensionPoint.class)) {
                String ret = extp.restrictByExpr(context, node);
                if (ret != null) {
                    return ret;
                }
            }
        } catch (RestrictByExprExtensionPoint.SkipThisRestrictExprException ignored) {
            return null;
        }

        if (node.getEntity() == null) {
            throw new ZQLError(String.format("the restrict by clause[%s %s %s] without entity name is not handled by any extension",
                    node.getField(), node.getOperator(), node.getValue()));
        }

        String srcTargetName = ZQLContext.peekQueryTargetInventoryName();
        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(srcTargetName);
        ZQLMetadata.InventoryMetadata dst = ZQLMetadata.findInventoryMetadata(node.getEntity());

        DBGraph.EntityVertex vertex = DBGraph.findVerticesWithSmallestWeight(src.inventoryAnnotation.mappingVOClass(), dst.inventoryAnnotation.mappingVOClass());
        if (vertex == null) {
            if (ZQLGlobalProperty.ERROR_IF_NO_DB_GRAPH_RELATION) {
                throw new ZQLError(String.format("invalid restrict by clause, inventory[%s] has no restriction to inventory[%s]",
                        node.getEntity(), src.simpleInventoryName()));
            } else {
                return null;
            }
        }

        String template = makeQueryTemplate(vertex, node.getField());
        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();
        template = String.format("(%s.%s IN %s)", src.simpleInventoryName(), primaryKey, template);
        return String.format(template, node.getOperator(), ((ASTNode)node.getValue()).accept(new ValueVisitor()));
    }

    private String makeQueryTemplate(DBGraph.EntityVertex vertex, String field) {
        if (vertex.next == null) {
            String entity = String.format("%s_", vertex.entityClass.getSimpleName());
            String vo = vertex.entityClass.getSimpleName();
            String key = vertex.previous.dstKey;
            return String.format("(SELECT %s.%s FROM %s %s WHERE %s.%s %%s %%s)",
                    entity, key, vo, entity, entity, field);
        }

        String value = makeQueryTemplate(vertex.next, field);
        String entity = String.format("%s_", vertex.entityClass.getSimpleName());
        String vo = vertex.entityClass.getSimpleName();
        String primaryKey = vertex.previous != null ? vertex.previous.dstKey : EntityMetadata.getPrimaryKeyField(vertex.entityClass).getName();
        return String.format("(SELECT %s.%s FROM %s %s WHERE %s.%s IN %s)",
                entity, primaryKey, vo, entity, entity, vertex.srcKey, value);
    }
}
