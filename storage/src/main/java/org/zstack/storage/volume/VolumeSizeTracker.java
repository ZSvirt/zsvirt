package org.zstack.storage.volume;

import java.util.Collection;

public interface VolumeSizeTracker {
    void trackVolume(String volUuid);

    void untrackVolume(String volUuid);

    void trackVolume(Collection<String> volUuids);

    void untrackVolume(Collection<String> volUuids);

    void reScanVolume();
}
