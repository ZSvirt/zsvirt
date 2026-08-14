package org.zstack.header.volume;

import static org.zstack.header.volume.MevocoVolumeConstants.VOLUME_QOS_TYPE_BANDWIDTH;
import static org.zstack.header.volume.MevocoVolumeConstants.VOLUME_QOS_TYPE_IOPS;

/**
 * Created by Qi Le on 2021/12/20
 */
public enum VolumeQosType {
    IOPS(VOLUME_QOS_TYPE_IOPS),
    BANDWIDTH(VOLUME_QOS_TYPE_BANDWIDTH);

    private final String type;

    VolumeQosType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
