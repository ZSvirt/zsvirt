package org.zstack.identity.header;

import org.zstack.core.db.Q;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/08/12
 */
public class ShareResourceContext {
    public Map<String, ResourceVO> uuidResourceMap = new HashMap<>();
    public List<ShareResourceItem> additionResources = new ArrayList<>();

    public static ShareResourceContext fromResources(List<String> resourceUuidList) {
        ShareResourceContext spec = new ShareResourceContext();

        if (!isEmpty(resourceUuidList)) {
            List<Tuple> tuples = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                .in(ResourceVO_.uuid, resourceUuidList)
                .listTuple();
            for (Tuple tuple : tuples) {
                ResourceVO resource = new ResourceVO(new Object[] {
                        tuple.get(0, String.class),
                        null,
                        tuple.get(1, String.class) });
                spec.uuidResourceMap.put(resource.getUuid(), resource);
            }
        }

        return spec;
    }

    public void additionResources(List<ResourceVO> resources, String permissionFrom) {
        if (isEmpty(resources)) {
            return;
        }

        Set<String> uuidSet = transformToSet(additionResources, item -> item.resource.getUuid());
        for (ResourceVO resource : resources) {
            if (uuidSet.contains(resource.getUuid())) {
                continue;
            }

            uuidSet.add(resource.getUuid());
            final ShareResourceItem result = new ShareResourceItem();
            result.resource = resource;
            result.permissionFrom = permissionFrom;
            additionResources.add(result);
        }
    }

    public Set<String> findAllSolitaryResources() {
        final HashMap<String, ResourceVO> maps = new HashMap<>(uuidResourceMap);
        additionResources.forEach(item -> maps.remove(item.resource.getUuid()));
        return transformToSet(maps.values(), ResourceVO::getUuid);
    }

    public Set<String> findAllMasterResources() {
        return transformToSet(additionResources, item -> item.permissionFrom);
    }

    public List<AccountResourceRefVO> buildShareToPublicRecords(String masterResource) {
        final List<ShareResourceItem> items = filter(additionResources,
                item -> Objects.equals(item.permissionFrom, masterResource));
        return transform(items, item -> {
            AccountResourceRefVO ref = new AccountResourceRefVO();
            ref.setType(AccessLevel.SharePublic);
            ref.setResourceUuid(item.resource.getUuid());
            ref.setResourceType(item.resource.getResourceType());
            ref.setResourcePermissionFrom(masterResource);
            return ref;
        });
    }

    public List<AccountResourceRefVO> buildShareAccountRecords(String masterResource, List<String> accountUuidList) {
        final List<ShareResourceItem> items = filter(additionResources,
                item -> Objects.equals(item.permissionFrom, masterResource));
        List<AccountResourceRefVO> results = new ArrayList<>();
        for (ShareResourceItem item : items) {
            for (String accountUuid : accountUuidList) {
                AccountResourceRefVO ref = new AccountResourceRefVO();
                ref.setType(AccessLevel.Share);
                ref.setAccountUuid(accountUuid);
                ref.setResourceUuid(item.resource.getUuid());
                ref.setResourceType(item.resource.getResourceType());
                ref.setResourcePermissionFrom(masterResource);
                results.add(ref);
            }
        }
        return results;
    }

    public List<AccountResourceRefVO> buildShareToPublicRecordsForSolitaryResources() {
        final HashMap<String, ResourceVO> maps = new HashMap<>(uuidResourceMap);
        additionResources.forEach(item -> maps.remove(item.resource.getUuid()));

        return transform(maps.values(), resource -> {
            AccountResourceRefVO ref = new AccountResourceRefVO();
            ref.setType(AccessLevel.SharePublic);
            ref.setResourceUuid(resource.getUuid());
            ref.setResourceType(resource.getResourceType());
            return ref;
        });
    }

    public List<AccountResourceRefVO> buildShareAccountRecordsForSolitaryResources(List<String> accountUuidList) {
        final HashMap<String, ResourceVO> maps = new HashMap<>(uuidResourceMap);
        additionResources.forEach(item -> maps.remove(item.resource.getUuid()));

        List<AccountResourceRefVO> results = new ArrayList<>();
        for (ResourceVO resource : maps.values()) {
            for (String accountUuid : accountUuidList) {
                AccountResourceRefVO ref = new AccountResourceRefVO();
                ref.setType(AccessLevel.Share);
                ref.setAccountUuid(accountUuid);
                ref.setResourceUuid(resource.getUuid());
                ref.setResourceType(resource.getResourceType());
                results.add(ref);
            }
        }
        return results;
    }

    public static class ShareResourceItem {
        public ResourceVO resource;
        /**
         * Equals to master resource UUID
         */
        public String permissionFrom;
    }
}
