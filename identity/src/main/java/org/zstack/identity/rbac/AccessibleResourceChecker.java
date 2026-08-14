package org.zstack.identity.rbac;

import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIResourceScope;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.ResourceHelper;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 24-03-13
 *
 * replace CheckIfAccountCanAccessResource class
 */
public class AccessibleResourceChecker {
    public final String accountUuid;
    private boolean allowGlobalReadableResource = false;
    private APIResourceScope scope = APIResourceScope.AllowedSharing;
    private Class<?> resourceType;

    private AccessibleResourceChecker(String accountUuid) {
        this.accountUuid = Objects.requireNonNull(accountUuid);
    }

    public static AccessibleResourceChecker forAccount(String accountUuid) {
        return new AccessibleResourceChecker(accountUuid);
    }

    public AccessibleResourceChecker allowGlobalReadableResource() {
        this.allowGlobalReadableResource = true;
        return this;
    }

    public AccessibleResourceChecker withScope(APIResourceScope scope) {
        this.scope = scope;
        return this;
    }

    public AccessibleResourceChecker withResourceType(Class<?> resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public boolean isAccessible(String resourceUuid) {
        return findOutAllInaccessibleResources(Collections.singletonList(resourceUuid)).isEmpty();
    }

    public List<String> findOutAllInaccessibleResources(List<String> resourceUuids) {
        if (isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }

        if (scope == APIResourceScope.AllowedAll) {
            return Collections.emptyList();
        }

        if (scope == APIResourceScope.MustOwner) {
            return findInaccessibleResourcesWithMustOwnerScope(resourceUuids);
        }

        if (!allowGlobalReadableResource) {
            return findInaccessibleResourcesWithAllowedSharingScope(resourceUuids);
        }

        if (resourceType != null) {
            if (RBAC.isResourceGlobalReadable(resourceType)) {
                // all resources are accessible.
                return Collections.emptyList();
            } else {
                return findInaccessibleResourcesWithAllowedSharingScope(resourceUuids);
            }
        }

        final List<Tuple> tuples = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.concreteResourceType)
                .in(ResourceVO_.uuid, resourceUuids)
                .listTuple();
        Set<String> types = transformToSet(tuples, tuple -> tuple.get(1, String.class));
        List<String> inaccessible = new ArrayList<>();

        for (String type : types) {
            if (RBAC.isResourceGlobalReadable(fromConcreteResourceType(type))) {
                continue;
            }

            final List<String> filterResources = transform(
                    filter(tuples, tuple -> tuple.get(1, String.class).equals(type)),
                            tuple -> tuple.get(0, String.class));
            inaccessible.addAll(findInaccessibleResourcesWithAllowedSharingScope(filterResources));
        }

        return inaccessible;
    }

    private List<String> findInaccessibleResourcesWithAllowedSharingScope(List<String> resourceUuids) {
        List<String> auuids = ResourceHelper.filterAccessibleResources(resourceUuids, accountUuid);
        return resourceUuids.stream().filter(uuid -> !auuids.contains(uuid)).collect(Collectors.toList());
    }

    private List<String> findInaccessibleResourcesWithMustOwnerScope(List<String> resourceUuids) {
        final List<String> ownResources = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.resourceUuid)
                .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .listValues();
        final ArrayList<String> results = new ArrayList<>(resourceUuids);
        results.removeAll(ownResources);
        return results;
    }

    private static Class<?> fromConcreteResourceType(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(String.format("ResourceVO.concreteResourceType[%s] is invalid", type), e);
        }
    }
}
