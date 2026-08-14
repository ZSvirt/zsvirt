package org.zstack.storage.backup.sftp;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 */

@GlobalPropertyDefinition
public class SftpBackupStorageGlobalProperty {
    @GlobalProperty(name="SftpBackupStorage.agentPackageName", defaultValue = "sftpbackupstorage-5.0.0.tar.gz")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="SftpBackupStorage.agentPort", defaultValue = "7171")
    public static int AGENT_PORT;
    @GlobalProperty(name="SftpBackupStorage.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="SftpBackupStorage.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="SftpBackupStorage.DownloadCmd.timeout", defaultValue = "7200")
    public static int DOWNLOAD_CMD_TIMEOUT;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
}
