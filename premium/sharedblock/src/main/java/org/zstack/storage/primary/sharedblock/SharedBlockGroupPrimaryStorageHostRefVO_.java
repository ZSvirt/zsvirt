package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SharedBlockGroupPrimaryStorageHostRefVO.class)
public class SharedBlockGroupPrimaryStorageHostRefVO_ extends PrimaryStorageHostRefVO_ {
    public static volatile SingularAttribute<SharedBlockGroupPrimaryStorageHostRefVO, Integer> hostId;
}
