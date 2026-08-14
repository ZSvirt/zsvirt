package org.zstack.header.storage.addon.backup;

import org.zstack.header.storage.backup.BackupStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ExternalBackupStorageVO.class)
public class ExternalBackupStorageVO_ extends BackupStorageVO_ {
	public static volatile SingularAttribute<ExternalBackupStorageVO, String> identity;
}
