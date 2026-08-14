package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SharedBlockGroupVO.class)
public class SharedBlockGroupVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<SharedBlockGroupVO, String> sharedBlockGroupType;
}
