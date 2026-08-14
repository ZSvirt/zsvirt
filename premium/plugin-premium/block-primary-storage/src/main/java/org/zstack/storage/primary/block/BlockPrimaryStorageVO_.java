package org.zstack.storage.primary.block;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/29 18:44
 */
@StaticMetamodel(BlockPrimaryStorageVO.class)
public class BlockPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<BlockPrimaryStorageVO, String> vendorName;
    public static volatile SingularAttribute<BlockPrimaryStorageVO, String> metadata;;
}
