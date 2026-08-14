package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(CephPrimaryStorageVO.class)
public class CephPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<CephPrimaryStorageVO, String> fsid;
    public static volatile SingularAttribute<CephPrimaryStorageVO, String> rootVolumePoolName;
    public static volatile SingularAttribute<CephPrimaryStorageVO, String> dataVolumePoolName;
    public static volatile SingularAttribute<CephPrimaryStorageVO, String> imageCachePoolName;
    public static volatile SingularAttribute<CephPrimaryStorageVO, String> userKey;
}
