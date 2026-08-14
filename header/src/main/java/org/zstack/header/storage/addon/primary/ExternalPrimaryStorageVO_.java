package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ExternalPrimaryStorageVO.class)
public class ExternalPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<ExternalPrimaryStorageVO, String> identity;
    public static volatile SingularAttribute<ExternalPrimaryStorageVO, String> config;
    public static volatile SingularAttribute<ExternalPrimaryStorageVO, String> addonInfo;
    public static volatile SingularAttribute<ExternalPrimaryStorageVO, String> defaultProtocol;
}
