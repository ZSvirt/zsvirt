package org.zstack.header.storage.primary.allocate;

import org.zstack.header.core.I18nMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.opaque.OpaqueCollection;
import org.zstack.utils.opaque.OpaqueScripts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class PrimaryStorageCandidate implements Serializable, OpaqueCollection {
    public static final String OPAQUE_IMAGE_CACHE_HIT = "image.cache.hit";
    public static final String OPAQUE_REQUIRED_SIZE = "required.size";

    public final PrimaryStorageVO primaryStorage;

    public List<String> recommendBy;
    public List<String> notRecommendBy;
    public String reject;
    public String rejectBy;
    public String rejectI18n;

    private LinkedHashMap<String, Object> opaque;

    public PrimaryStorageCandidate(PrimaryStorageVO primaryStorage) {
        this.primaryStorage = Objects.requireNonNull(primaryStorage);
    }

    public String getUuid() {
        return primaryStorage.getUuid();
    }

    public void markAsRecommended(String flowName) {
        recommendBy = recommendBy == null ? new ArrayList<>() : recommendBy;
        recommendBy.add(flowName);
    }

    public void markAsNotRecommended(String flowName) {
        notRecommendBy = notRecommendBy == null ? new ArrayList<>() : notRecommendBy;
        notRecommendBy.add(flowName);
    }

    public void markAsRejected(Class<?> flowClazz, I18nMessage reason) {
        markAsRejected(flowClazz.getSimpleName(), reason);
    }

    public void markAsRejected(String flowName, String reason) {
        rejectBy = flowName;
        reject = reason;
    }

    public void markAsRejected(String flowName, I18nMessage reason) {
        rejectBy = flowName;
        reject = reason.getDetails();
        rejectI18n = reason.getI18nDetails();
    }

    @Override
    public PrimaryStorageCandidate withOpaque(String key, Object value) {
        if (opaque == null) {
            opaque = new LinkedHashMap<>();
        }
        opaque.put(key, value);
        return this;
    }

    @Override
    public PrimaryStorageCandidate withOpaque(OpaqueScripts scripts) {
        OpaqueCollection.super.withOpaque(scripts);
        return this;
    }

    @Override
    public Object getFromOpaque(String key) {
        return opaque == null ? null : opaque.get(key);
    }

    @Override
    public String toString() {
        return primaryStorage.getUuid();
    }

    public RejectedCandidate toRejectedCandidate() {
        return new RejectedCandidate(primaryStorage.getUuid(), primaryStorage.getName(), reject, rejectI18n, rejectBy);
    }

    public static class RejectedCandidate implements I18nMessage {
        public final String primaryStorageUuid;
        public final String primaryStorageName;
        public final String reject;
        public String rejectI18n;
        public final String rejectBy;

        private RejectedCandidate(String primaryStorageUuid, String primaryStorageName, String reject, String rejectI18n, String rejectBy) {
            this.primaryStorageUuid = primaryStorageUuid;
            this.primaryStorageName = primaryStorageName;
            this.reject = reject;
            this.rejectI18n = rejectI18n;
            this.rejectBy = rejectBy;
        }

        @Override
        public String getDetails() {
            return reject;
        }

        @Override
        public String getI18nDetails() {
            return rejectI18n;
        }
    }
}
