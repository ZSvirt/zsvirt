package org.zstack.iam1.compute.ensemble;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DBGraph;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.description.ensemble.ResourceEnsembleMember;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.iam1.header.ensemble.ResourceEnsembleInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/08/07
 */
public class ResourceEnsembleHelper {
    private ResourceEnsembleHelper() {}

    public static boolean inEnsemble(Class<?> resourceType) {
        if (resourceType == Object.class) {
            return false;
        }

        return RBAC.ensembleMembers.stream()
                .anyMatch(c -> c.getClazz().isAssignableFrom(resourceType));
    }

    public static boolean isEnsembleMasterType(Class<?> resourceType) {
        if (resourceType == Object.class) {
            return false;
        }

        return RBAC.ensembleMembers.stream()
                .filter(c -> c.getClazz().isAssignableFrom(resourceType))
                .anyMatch(c -> c.getParent() == null);
    }

    public static ResourceEnsembleInfo findResourceEnsemble(String resourceUuid) {
        String concreteResourceType = Q.New(ResourceVO.class)
                .eq(ResourceVO_.uuid, resourceUuid)
                .select(ResourceVO_.concreteResourceType)
                .findValue();
        if (concreteResourceType == null) {
            return null;
        }

        try {
            return findResourceEnsemble(resourceUuid, Class.forName(concreteResourceType));
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(String.format(
                    "invalid concreteResourceType[%s] for resource[uuid=%s]", concreteResourceType, resourceUuid), e);
        }
    }

    @Transactional(readOnly = true)
    public static ResourceEnsembleInfo findResourceEnsemble(String resourceUuid, Class<?> resourceType) {
        ResourceEnsembleMember member = RBAC.ensembleMembers.stream()
                .filter(c -> c.getClazz().isAssignableFrom(resourceType))
                .findAny()
                .orElse(null);
        if (member == null) {
            return null;
        }

        ResourceEnsembleInfo childInfo = new ResourceEnsembleInfo();
        childInfo.metadata = member;
        childInfo.uuid = resourceUuid;

        return findResourceEnsemble(resourceUuid, childInfo);
    }

    private static ResourceEnsembleInfo findResourceEnsemble(String resourceUuid, ResourceEnsembleInfo childInfo) {
        final ResourceEnsembleMember member = childInfo.metadata;

        if (member.getParent() == null) {
            return childInfo;
        }

        ResourceEnsembleMember parent = member.getParent();
        if (member.getFindParentByChildUuid() == null) {
            member.setFindParentByChildUuid(childParentMap ->
                    defaultFindParentSQLBuilder(childParentMap, member.getClazz(), parent.getClazz()));
        }

        Map<String, String> childParentMap = new HashMap<>();
        childParentMap.put(resourceUuid, null);
        member.getFindParentByChildUuid().accept(childParentMap);
        
        String parentUuid = childParentMap.get(resourceUuid);
        if (parentUuid == null) {
            return null;
        }

        ResourceEnsembleInfo info = new ResourceEnsembleInfo();
        info.metadata = parent;
        info.uuid = parentUuid;
        info.children.add(childInfo);
        childInfo.parent = info;

        return findResourceEnsemble(parentUuid, info);
    }

    private static void defaultFindParentSQLBuilder(Map<String, String> childParentMap, Class<?> from, Class<?> to) {
        final DBGraph.EntityVertex weight = DBGraph.findVerticesWithSmallestWeight(from, to);
        if (weight == null) {
            throw new CloudRuntimeException(
                    String.format("failed to build query SQL script from class %s to class %s", from, to));
        }

        String sql = weight.toSQL("uuid", SimpleQuery.Op.EQ, ":childUuid");
        for (Map.Entry<String, String> entry : childParentMap.entrySet()) {
            String childUuid = entry.getKey();

            List<String> parentUuidList = SQL.New(sql, String.class)
                    .param("childUuid", childUuid)
                    .list();
            if (parentUuidList.isEmpty()) {
                continue;
            }

            entry.setValue(parentUuidList.get(0));
        }
    }

    @Transactional(readOnly = true)
    public static ResourceEnsembleInfo findAllChildrenResources(String resourceUuid, Class<?> resourceType) {
        ResourceEnsembleMember member = RBAC.ensembleMembers.stream()
                .filter(c -> c.getClazz().isAssignableFrom(resourceType))
                .findAny()
                .orElse(null);
        if (member == null) {
            return null;
        }

        ResourceEnsembleInfo parentInfo = new ResourceEnsembleInfo();
        parentInfo.metadata = member;
        parentInfo.uuid = resourceUuid;
        findAllChildren(list(parentInfo));
        return parentInfo;
    }

    /**
     * @param parents all items must have the same metadata
     */
    private static void findAllChildren(List<ResourceEnsembleInfo> parents) {
        final ResourceEnsembleMember parentMetadata = parents.get(0).metadata;
        final List<ResourceEnsembleMember> childrenMetadata = parentMetadata.getChildren();
        if (childrenMetadata.isEmpty()) {
            return;
        }

        for (ResourceEnsembleMember childMetadata : childrenMetadata) {
            if (childMetadata.getFindChildrenByParentUuid() == null) {
                childMetadata.setFindChildrenByParentUuid(childParentMap ->
                        defaultFindChildrenSQLBuilder(childParentMap, parentMetadata.getClazz(), childMetadata.getClazz()));
            }

            Map<String, List<String>> parentChildrenMap = new HashMap<>();
            parents.forEach(parent -> parentChildrenMap.put(parent.uuid, null));
            childMetadata.getFindChildrenByParentUuid().accept(parentChildrenMap);

            List<ResourceEnsembleInfo> children = new ArrayList<>();
            parentChildrenMap.forEach((parentUuid, childUuidList) -> {
                ResourceEnsembleInfo parent = findOneOrNull(parents, p -> Objects.equals(p.uuid, parentUuid));

                for (String childUuid : childUuidList) {
                    ResourceEnsembleInfo info = new ResourceEnsembleInfo();
                    info.metadata = childMetadata;
                    info.parent = parent;
                    info.uuid = childUuid;
                    parent.children.add(info);
                    children.add(info);
                }
            });

            if (children.isEmpty()) {
                continue;
            }

            findAllChildren(children);
        }
    }

    private static void defaultFindChildrenSQLBuilder(Map<String, List<String>> parentChildrenMap, Class<?> from, Class<?> to) {
        final DBGraph.EntityVertex weight = DBGraph.findVerticesWithSmallestWeight(from, to);
        if (weight == null) {
            throw new CloudRuntimeException(
                    String.format("failed to build query SQL script from class %s to class %s", from, to));
        }

        String sql = weight.toSQL("uuid", SimpleQuery.Op.EQ, ":parentUuid");
        for (Map.Entry<String, List<String>> entry : parentChildrenMap.entrySet()) {
            String parentUuid = entry.getKey();

            List<String> childrenUuidList = SQL.New(sql, String.class)
                    .param("parentUuid", parentUuid)
                    .list();
            entry.setValue(childrenUuidList);
        }
    }
}
