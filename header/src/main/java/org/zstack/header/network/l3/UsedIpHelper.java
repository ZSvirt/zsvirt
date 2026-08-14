package org.zstack.header.network.l3;

import java.util.Collection;
import java.util.Objects;

public class UsedIpHelper {
    public static UsedIpVO getOrRecreateForIpChange(UsedIpVO current, String newIp, String newIpRangeUuid,
                                                    Collection<UsedIpVO> deleteSet) {
        if (current == null || current.getUuid() == null) {
            return current == null ? new UsedIpVO() : current;
        }

        if (!Objects.equals(current.getIp(), newIp) || !Objects.equals(current.getIpRangeUuid(), newIpRangeUuid)) {
            if (deleteSet == null) {
                throw new IllegalArgumentException("deleteSet cannot be null when recreating UsedIpVO");
            }

            deleteSet.add(current);
            return new UsedIpVO();
        }

        return current;
    }
}
