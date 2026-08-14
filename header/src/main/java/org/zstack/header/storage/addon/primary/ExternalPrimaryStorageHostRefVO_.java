package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ExternalPrimaryStorageHostRefVO.class)
public class ExternalPrimaryStorageHostRefVO_ extends PrimaryStorageHostRefVO_ {
    public static volatile SingularAttribute<ExternalPrimaryStorageHostRefVO, Integer> hostId;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostRefVO, String> protocol;
}
