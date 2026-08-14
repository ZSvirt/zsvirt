package org.zstack.storage.backup.imagestore;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;
import org.zstack.core.validator.IsJson;
import org.zstack.storage.ceph.CephConstants;

import java.util.List;

@GlobalPropertyDefinition
public class ImageStoreBackupStorageGlobalProperty {
    @GlobalProperty(name="ImageStoreBackupStorage.agentPackageName", defaultValue = "zstack-store.bin")
    public static String AGENT_PACKAGE_NAME;
    @GlobalProperty(name="ImageStoreBackupStorage.agentClientPackageName", defaultValue = "zstack-store-client.bin")
    public static String AGENT_CLIENT_PACKAGE_NAME;
    @GlobalProperty(name="ImageStoreBackupStorage.qemuPackageName", defaultValue = "zstack-qemu-ga.bin")
    public static String QEMU_PACKAGE_NAME;
    @GlobalProperty(name="ImageStoreBackupStorage.agentPort", defaultValue = "8001")
    public static int AGENT_PORT;
    @GlobalProperty(name="ImageStoreBackupStorage.agentUrlScheme", defaultValue = "http")
    public static String AGENT_URL_SCHEME;
    @GlobalProperty(name="ImageStoreBackupStorage.agentUrlRootPath", defaultValue = "")
    public static String AGENT_URL_ROOT_PATH;

    @GlobalProperty(name="ImageStoreBackupStorage.clientCmdPath", defaultValue = "/usr/local/zstack/imagestore/bin/zstcli")
    public static String REGISTRY_CMD;
    @GlobalProperty(name="ImageStoreBackupStorage.certs", defaultValue = "/var/lib/zstack/imagestorebackupstorage/package/certs/ca.pem")
    public static String REGISTRY_CERTS;
    @GlobalProperty(name="ImageStoreBackupStorage.registryPort", defaultValue = "8000")
    public static int REGISTRY_PORT;
    @GlobalProperty(name="ImageStoreBackupStorage.pushPath", defaultValue = "/v1/push")
    public static String REGISTRY_PUSH_PATH;
    @GlobalProperty(name="ImageStoreBackupStorage.pullPath", defaultValue = "/v1/pull")
    public static String REGISTRY_PULL_PATH;
    @GlobalProperty(name="ImageStoreBackupStorage.getTaskPath", defaultValue = "/v1/tasks")
    public static String REGISTRY_TASK_PATH;

    // format like [{"PS":"Ceph", "priority":"5"},{"PS":"LocalStorage", "priority":"10"}], default priority value is 10
    @GlobalProperty(name="imagestore.backupstorage.primary.storage.priority", defaultValue = "[{\"PS\":\"" + CephConstants.CEPH_PRIMARY_STORAGE_TYPE + "\", \"priority\":\"100\"}]")
    @IsJson()
    public static String IMAGESTOREBACKUPSTORAGE_BACKUPSTORAGE_PRIORITY;

    @GlobalProperty(name="ImageStoreBackupStorage.uploadQueueSize", defaultValue = "20")
    public static int UPLOAD_QUEUE_SIZE;

    @GlobalProperty(name = "ImageStoreBackupStorage.enableQuota", defaultValue = "false")
    public static boolean ENABLE_QUOTA;

    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;

}
