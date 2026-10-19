package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class LocalStorageCandidateRefs {
    static final String OPAQUE_HOST_REFS = "local.host.refs";

    private LocalStorageCandidateRefs() {
    }

    @SuppressWarnings("unchecked")
    static List<LocalStorageHostRefVO> get(PrimaryStorageCandidate candidate) {
        Object value = candidate.getFromOpaque(OPAQUE_HOST_REFS);
        if (value == null) {
            return Collections.emptyList();
        }
        return (List<LocalStorageHostRefVO>) value;
    }

    static void set(PrimaryStorageCandidate candidate, List<LocalStorageHostRefVO> refs) {
        candidate.withOpaque(OPAQUE_HOST_REFS, new ArrayList<>(refs));
    }
}
