package org.zstack.storage.ceph.backup;

import org.zstack.storage.ceph.CephMonAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(CephBackupStorageMonVO.class)
public class CephBackupStorageMonVO_ extends CephMonAO_ {
    public static volatile SingularAttribute<CephBackupStorageMonVO, String> backupStorageUuid;
}
