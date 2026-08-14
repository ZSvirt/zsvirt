package org.zstack.header.volume;

import java.util.Arrays;

import static org.zstack.header.volume.MevocoVolumeConstants.*;

/**
 * Created by Qi Le on 2021/12/20
 */
public enum VolumeQosMode {
    READ(VOLUME_QOS_MODE_READ),
    WRITE(VOLUME_QOS_MODE_WRITE),
    ALL(VOLUME_QOS_MODE_ALL),
    TOTAL(VOLUME_QOS_MODE_TOTAL),
    OVERWRITE(VOLUME_QOS_MODE_OVERWRITE);

    private final String mode;

    VolumeQosMode(String qosMode) {
        mode = qosMode;
    }

    public String getMode() {
        return mode;
    }

    public static VolumeQosMode getQosMode(String qosMode) {
        return Arrays.stream(VolumeQosMode.values())
                .filter(v -> v.getMode().equals(qosMode))
                .findAny().orElse(null);
    }
}
