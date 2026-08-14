package org.zstack.storage.backup.sftp;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by shixin.ruan on 2020/04/09.
 */
@GlobalConfigDefinition
public class SftpBackupStorageGlobalConfig {
    public static final String CATEGORY = "sftp";

    @GlobalConfigValidation
    public static GlobalConfig SFTP_ALLOW_PORTS = new GlobalConfig(CATEGORY, "sftp.allow.ports");
}
