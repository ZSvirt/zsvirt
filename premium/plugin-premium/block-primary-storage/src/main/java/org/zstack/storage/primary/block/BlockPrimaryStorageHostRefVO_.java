package org.zstack.storage.primary.block;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/1 01:09
 */
@StaticMetamodel(BlockPrimaryStorageHostRefVO.class)
public class BlockPrimaryStorageHostRefVO_ extends PrimaryStorageHostRefVO_ {
    public static volatile SingularAttribute<BlockPrimaryStorageHostRefVO, String> initiatorName;
    public static volatile SingularAttribute<BlockPrimaryStorageHostRefVO, String> metadata;
}
