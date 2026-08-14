package org.zstack.storage.primary.block;

import org.zstack.header.storageDevice.ScsiLunVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/8 14:21
 */
@StaticMetamodel(BlockScsiLunVO.class)
public class BlockScsiLunVO_ extends ScsiLunVO_ {
    public static volatile SingularAttribute<BlockScsiLunVO, Integer> id;
    public static volatile SingularAttribute<BlockScsiLunVO, String> volumeUuid;
    public static volatile SingularAttribute<BlockScsiLunVO, String> target;
    public static volatile SingularAttribute<BlockScsiLunVO, String> lunType;
    public static volatile SingularAttribute<BlockScsiLunVO, Long> usedSize;
    public static volatile SingularAttribute<BlockScsiLunVO, Integer> lunMapId;
    public static volatile SingularAttribute<BlockScsiLunVO, Integer> lunInitSnapshotID;
}
