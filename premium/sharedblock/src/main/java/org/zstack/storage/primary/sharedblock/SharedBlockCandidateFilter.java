package org.zstack.storage.primary.sharedblock;

import org.zstack.core.db.SQL;
import org.zstack.header.storageDevice.LunAO;
import org.zstack.utils.BeanUtils;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.isEmpty;

class SharedBlockCandidateFilter {
    private static final int LUN_QUERY_BATCH_SIZE = 500;
    private static final String LUN_STATE_ENABLED = "Enabled";
    private volatile List<String> lunRootEntityNames;

    void retainManagedLunCandidates(List<SharedBlockCandidateStruct> candidates) {
        if (isEmpty(candidates)) {
            return;
        }

        Set<String> storageDeviceLunWwids = getStorageDeviceLunWwids(candidates);
        candidates.removeIf(candidate -> !containsDiskIdentifier(storageDeviceLunWwids, candidate.wwid));
    }

    private Set<String> getStorageDeviceLunWwids(Collection<SharedBlockCandidateStruct> candidates) {
        Set<String> deviceWwids = new HashSet<>();
        for (SharedBlockCandidateStruct candidate : candidates) {
            addDiskIdentifier(deviceWwids, candidate.wwid);
        }

        if (deviceWwids.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> lunWwids = new HashSet<>();
        for (String entityName : getLunRootEntityNames()) {
            addMatchedLunWwids(lunWwids, entityName, deviceWwids);
        }

        return lunWwids;
    }

    private void addMatchedLunWwids(Set<String> lunWwids, String entityName, Set<String> deviceWwids) {
        List<String> queryWwids = new ArrayList<>(deviceWwids);
        for (int start = 0; start < queryWwids.size(); start += LUN_QUERY_BATCH_SIZE) {
            int end = Math.min(start + LUN_QUERY_BATCH_SIZE, queryWwids.size());
            List<String> batchWwids = queryWwids.subList(start, end);
            List<String> matchedWwids = SQL.New(String.format("select lun.wwid from %s lun where lower(lun.wwid) in (:wwids) and lun.state = :enabledState",
                            entityName), String.class)
                    .param("wwids", batchWwids)
                    .param("enabledState", LUN_STATE_ENABLED)
                    .list();
            if (isEmpty(matchedWwids)) {
                continue;
            }

            for (String wwid : matchedWwids) {
                addDiskIdentifier(lunWwids, wwid);
            }
        }
    }

    private List<String> getLunRootEntityNames() {
        List<String> entityNames = lunRootEntityNames;
        if (entityNames != null) {
            return entityNames;
        }

        synchronized (this) {
            if (lunRootEntityNames == null) {
                lunRootEntityNames = discoverLunRootEntityNames();
            }
            return lunRootEntityNames;
        }
    }

    private List<String> discoverLunRootEntityNames() {
        Set<Class<?>> lunEntityClasses = new HashSet<>();
        for (Class<?> entityClass : BeanUtils.reflections.getTypesAnnotatedWith(Entity.class)) {
            if (entityClass.isAnnotationPresent(Entity.class) && LunAO.class.isAssignableFrom(entityClass)) {
                lunEntityClasses.add(entityClass);
            }
        }

        List<String> rootEntityNames = new ArrayList<>();
        for (Class<?> entityClass : lunEntityClasses) {
            Class<?> superClass = entityClass.getSuperclass();
            if (superClass != null && lunEntityClasses.contains(superClass)) {
                continue;
            }

            rootEntityNames.add(getEntityName(entityClass));
        }

        Collections.sort(rootEntityNames);
        return Collections.unmodifiableList(rootEntityNames);
    }

    private String getEntityName(Class<?> entityClass) {
        Entity entity = entityClass.getAnnotation(Entity.class);
        return entity == null || entity.name().trim().isEmpty() ? entityClass.getSimpleName() : entity.name();
    }

    private boolean containsDiskIdentifier(Set<String> identifiers, String identifier) {
        String normalizedIdentifier = normalizeDiskIdentifier(identifier);
        return normalizedIdentifier != null && identifiers.contains(normalizedIdentifier);
    }

    private void addDiskIdentifier(Set<String> identifiers, String identifier) {
        String normalizedIdentifier = normalizeDiskIdentifier(identifier);
        if (normalizedIdentifier != null) {
            identifiers.add(normalizedIdentifier);
        }
    }

    private String normalizeDiskIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }

        String trimmedIdentifier = identifier.trim();
        if (trimmedIdentifier.isEmpty()) {
            return null;
        }

        return trimmedIdentifier.toLowerCase(Locale.ROOT);
    }
}
