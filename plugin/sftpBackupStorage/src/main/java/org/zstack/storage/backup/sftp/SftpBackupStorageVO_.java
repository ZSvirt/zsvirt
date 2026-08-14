package org.zstack.storage.backup.sftp;

import org.zstack.header.storage.backup.BackupStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SftpBackupStorageVO.class)
public class SftpBackupStorageVO_ extends BackupStorageVO_ {
	public static volatile SingularAttribute<SftpBackupStorageVO, String> username;
	public static volatile SingularAttribute<SftpBackupStorageVO, String> hostname;
	public static volatile SingularAttribute<SftpBackupStorageVO, Integer> sshPort;
}
